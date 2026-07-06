import { inject, Injectable } from '@angular/core';
import {
  AuthCode,
  AuthResponse,
  convertDecodeToUserAuth,
  convertPrincipalToUserAuth,
  UserAuth,
} from '../models/auth';
import { ACCESS_TOKEN, CLIENT_ID, REFRESH_TOKEN } from '../models/constants';
import { CookieService } from 'ngx-cookie-service';
import {
  BehaviorSubject,
  catchError,
  combineLatest,
  distinctUntilChanged,
  EMPTY,
  filter,
  first,
  map,
  Observable,
  shareReplay,
  Subject,
  Subscription,
  switchMap,
  takeUntil,
  tap,
  timer,
} from 'rxjs';
import { DecodeTokens, haveRoles, JWTTokens, TokenDecode } from '../models/decode';
import { jwtDecode } from 'jwt-decode';
import { v7, validate, version } from 'uuid';
import { AuthService } from '../api/auth-service';
import { NavigationEnd, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
/** Central reactive auth state manager for the Angular application.
 *
 * Manages JWT tokens (access + refresh) with pre-emptive rotation scheduling,
 * client session identification via UUIDv7, and user authentication state
 * through reactive BehaviorSubjects.
 *
 * Architecture:
 * - {@link jwtState} — drives the auth interceptor for Bearer token injection
 * - {@link decodeState} — drives auth guards (isAuthenticated, role checks)
 * - {@link clientId} — OAuth2 PKCE client session identifier
 * - {@link currentUser} — user profile data for the UI
 * - {@link ATTimer} — schedules token rotation before the access token expires */
export class AuthenticationService {
  private storage = inject(CookieService);
  private authService = inject(AuthService);
  private readonly router = inject(Router);
  private destroy = new Subject<void>();

  /** Current JWT access and refresh tokens. Drives the auth interceptor for Bearer header injection. */
  private jwtState = new BehaviorSubject<JWTTokens | null>(null);
  /** Decoded JWT payloads with role and expiration info. Drives auth guards and user role checks. */
  private decodeState = new BehaviorSubject<DecodeTokens | null>(null);

  /** UUIDv7 client session identifier for OAuth2 PKCE and session tracking. */
  private clientId = new BehaviorSubject<string | null>(null);

  /** Authenticated user profile data consumed by UI components. */
  private currentUser = new BehaviorSubject<UserAuth | null>(null);

  /** Subscription to the pre-emptive token rotation timer. */
  private ATTimer?: Subscription;
  /** Buffer in milliseconds before access token expiry to trigger token rotation (3 seconds). */
  private readonly REFRESH_THREEHOLD_MS = 3000;

  /** Attempts to restore authentication state from persisted cookies on application initialisation. */
  constructor() {
    this.recoverStates();
  }

  /** Attempts to restore auth state from cookies on app initialisation if the user is not already authenticated.
   *
   * Reads tokens from cookie storage and sets them as in-memory-only (no re-persist to cookies).
   * Only runs when the current authentication state is false. */
  public recoverStates(): void {
    this.isAuthenticated()
      .pipe(
        first(),
        filter((a) => !a),
        takeUntil(this.destroy),
      )
      .subscribe((a) => {
        if (!a) {
          const tokens = this.getTokensFromStorage();
          if (tokens != null) {
            this.setAuthTokens(tokens, true);
          }
        }
      });
  }

  /** Checks whether a decoded token is still valid with a 10-second leeway to avoid edge-case expirations.
   * @param decode - The decoded token payload to validate.
   * @returns True if the token has a subject and has not expired (plus leeway). */
  private isValidTokenDecode(decode: TokenDecode): boolean {
    const now = Date.now() / 1000;
    const LEEWAY = 10;
    return decode.exp > (now + LEEWAY) && !!decode.sub;
  }

  /** Emits the current authentication status based on whether a non-null decode state exists.
   * @returns An observable that emits true when authenticated, false otherwise. */
  public isAuthenticated(): Observable<boolean> {
    return this.decodeState.asObservable().pipe(
      map((a) => a !== null),
      distinctUntilChanged(),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
  }

  /** Provides the current decoded token state for authentication checks and user role verification.
   * @returns An observable of the current decoded access and refresh token payloads. */
  public getDecodeState(): Observable<DecodeTokens | null> {
    return this.decodeState
      .asObservable()
      .pipe(distinctUntilChanged(), shareReplay({ bufferSize: 1, refCount: true }));
  }

  /** Emits the current raw JWT tokens paired with the client session identifier.
   * @returns An observable tuple of the JWT tokens and the client ID. */
  public getJWTState(): Observable<[JWTTokens | null, string | null]> {
    return combineLatest([this.jwtState, this.clientId]).pipe(
      distinctUntilChanged(),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
  }

  /** Checks whether the current access token grants all of the specified roles.
   * @param roles - The role names to check against the access token.
   * @returns An observable that emits true if all roles are present in the decoded access token. */
  public haveUserRoles(roles: string[]) {
    return this.getDecodeState().pipe(
      map((i) => (i?.access ? haveRoles(i?.access, roles) : false)),
    );
  }

  /** Provides the current client session identifier.
   * @returns An observable of the UUIDv7 client ID, or null if not set. */
  public getClientID(): Observable<string | null> {
    return this.clientId
      .asObservable()
      .pipe(distinctUntilChanged(), shareReplay({ bufferSize: 1, refCount: true }));
  }

  /** Provides the current authenticated user profile data.
   * @returns An observable of the user profile, or null if not authenticated. */
  public getUserInfo(): Observable<UserAuth | null> {
    return this.currentUser
      .asObservable()
      .pipe(distinctUntilChanged(), shareReplay({ bufferSize: 1, refCount: true }));
  }

  /** Reads JWT tokens from cookie storage and ensures the client ID is initialised.
   * @returns The stored auth tokens, or null if either token is missing or empty. */
  private getTokensFromStorage(): AuthResponse | null {
    const a = this.storage.get(ACCESS_TOKEN);
    const b = this.storage.get(REFRESH_TOKEN);
    this.manageClientID();

    if (a == null || b == null || a === '' || b === '') {
      return null;
    }
    return { accessToken: a, refreshToken: b };
  }

  /** Sets authentication tokens and schedules pre-emptive rotation before the access token expires.
   *
   * Decodes both tokens, validates them, updates all reactive state subjects, and sets up
   * a timer that calls {@link rotateTokens} at (expiry - {@link REFRESH_THREEHOLD_MS}).
   * When `isRecovered` is false the tokens are persisted to secure cookies; when true they
   * are kept in memory only (recovery path).
   * @param tokens - The access and refresh token pair from the auth response.
   * @param isRecovered - Whether these tokens come from cookie recovery (true = no cookie write). */
  public setAuthTokens(tokens: AuthResponse, isRecovered: boolean): void {
    const AToken = jwtDecode<TokenDecode>(tokens.accessToken);
    const RToken = jwtDecode<TokenDecode>(tokens.refreshToken);
    if (this.isValidTokenDecode(AToken) && this.isValidTokenDecode(RToken)) {
      this.jwtState.next({ access: tokens.accessToken, refresh: tokens.refreshToken });
      this.decodeState.next({ access: AToken, refresh: RToken });
      this.currentUser.next(convertDecodeToUserAuth(AToken));

      const now = Date.now();
      const ATExp = new Date(AToken.exp * 1000);
      const delay = ATExp.getTime() - now - this.REFRESH_THREEHOLD_MS;
      this.ATTimer?.unsubscribe();
      this.ATTimer = timer(Math.max(delay, 0))
        .pipe(takeUntil(this.destroy), switchMap(() => this.rotateTokens()))
        .subscribe();

      if (!isRecovered) {
        this.storage.set(
          ACCESS_TOKEN,
          tokens.accessToken,
          ATExp,
          '/',
          undefined,
          true,
          'Lax',
          false,
        );
        this.storage.set(
          REFRESH_TOKEN,
          tokens.refreshToken,
          ATExp,
          '/',
          undefined,
          true,
          'Lax',
          false,
        );
      }
      this.manageClientID();
    }
  }

  /** Clears all authentication state by resetting reactive subjects and removing persisted cookies.
   *
   * The client session identifier is intentionally preserved to maintain session continuity. */
  public cleanStates(): void {
    this.jwtState.next(null);
    this.decodeState.next(null);
    this.currentUser.next(null);
    this.storage.delete(ACCESS_TOKEN, '/', undefined, true, 'Lax');
    this.storage.delete(REFRESH_TOKEN, '/', undefined, true, 'Lax');
    // The clientId is used to check the session
    // this.clientId.next(null)
    // this.storage.delete(CLIENT_ID, "/", undefined, true, "Lax")
  }

  /** Revokes the current session on the server then clears all local authentication state.
   *
   * On 400 or 401 responses from the server the local state is still cleaned up
   * to handle already-invalidated or expired sessions gracefully.
   * @returns An observable that completes when the logout request finishes. */
  public logout(): Observable<void> {
    return this.getJWTState().pipe(
      first(),
      filter((i) => i[0] != null && i[1] != null),
      switchMap((v) => this.authService.logout(v[0]?.refresh || '', v[1] || '')),
      tap({
        next: () => {
          this.cleanStates();
        },
        error: (error: HttpErrorResponse) => {
          if(error.status === 400 || error.status == 401) {
            this.cleanStates()
          }
        }
      }),
    );
  }

  /** Performs a pre-emptive token refresh before the current access token expires.
   *
   * Reads the current JWT and client ID, calls the rotation API, then updates state
   * by clearing before re-setting the new tokens.
   * @returns An observable that emits the new auth response with rotated tokens. */
  public rotateTokens(): Observable<AuthResponse> {
    return this.getJWTState().pipe(
      first(),
      filter((i) => i[0] != null && i[1] != null),
      switchMap((i) => this.authService.rotateTokens(i[0]?.refresh || '', i[1] || '')),
      tap((a) => {
        if (a) {
          this.cleanStates();
          this.setAuthTokens(a, false);
        }
      }),
      shareReplay(1)
    );
  }

  /** Fetches the latest user profile from the API and updates the current user state.
   * @returns An observable that completes when the user info has been refreshed. */
  public updateUserInfo(): Observable<void> {
    return this.getJWTState().pipe(
      filter((i) => i[0] != null && i[1] != null),
      switchMap(() => this.authService.getUserInfo()),
      map((i) => i && this.currentUser.next(convertPrincipalToUserAuth(i))),
      catchError(() => EMPTY),
    );
  }

  /** Validates that a string is a UUID version 7.
   * @param uuid - The string to validate.
   * @returns True if the string is a valid UUIDv7. */
  private uuidValidateV7(uuid: string) {
    return validate(uuid) && version(uuid) === 7;
  }

  /** Ensures a valid UUIDv7 client session identifier exists, generating and storing one if missing or invalid. */
  private manageClientID(): void {
    const c = this.storage.get(CLIENT_ID);
    if (c === null || c === '' || !this.uuidValidateV7(c)) {
      const id = v7();
      this.clientId.next(id);
      this.storage.set(CLIENT_ID, id, undefined, '/', undefined, true, 'Lax', false);
    } else {
      this.clientId.next(c);
    }
  }

  /** Exchanges an OAuth2 authorisation code for tokens via the PKCE flow.
   *
   * Clears any existing state before setting the new tokens.
   * @param code - The authorisation code received from the OAuth2 provider.
   * @returns An observable that emits the auth response from the code exchange. */
  public exchangeCode(code: AuthCode) {
    return this.authService.exchangeCode(code).pipe(
      tap({
        next: (i) => {
          this.cleanStates();
          this.setAuthTokens(i, false);
        },
      }),
    );
  }

  /** Convenience observable that checks whether the current user has the ADMIN role.
   * @returns An observable that emits true if the user has the ADMIN role. */
  public isAdmin(): Observable<boolean> {
    return this.haveUserRoles(['ADMIN']);
  }

  /** Emits the current URL (after redirects) on each completed navigation.
   * @returns An observable that emits the redirected URL string on every NavigationEnd event. */
  public actualRoute() {
    return this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      takeUntil(this.destroy),
      map((e) => e.urlAfterRedirects),
    );
  }
}
