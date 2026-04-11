import { inject, Injectable } from '@angular/core';
import { User } from '../models/user';
import { AuthResponse } from '../models/auth';
import { ACCESS_TOKEN, CLIENT_ID, REFRESH_TOKEN } from '../models/constants';
import { CookieService } from 'ngx-cookie-service'
import { BehaviorSubject, catchError, combineLatest, distinctUntilChanged, EMPTY, filter, first, map, Observable, shareReplay, Subject, Subscription, switchMap, takeUntil, tap, timer } from 'rxjs';
import { DecodeTokens, JWTTokens, TokenDecode } from '../models/decode';
import { jwtDecode } from 'jwt-decode'
import { v7, validate, version } from 'uuid'
import { APIAuthService } from '../api/auth-service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private storage = inject(CookieService)
  private authService = inject(APIAuthService)
  private destroy = new Subject<void>()

  // Useful for Auth Interceptor
  private jwtState = new BehaviorSubject<JWTTokens | null>(null)
  // Usefull to check expiration of JWTs and minimal user info
  private decodeState = new BehaviorSubject<DecodeTokens | null>(null)

  // Useful to track a session
  private clientId = new BehaviorSubject<string | null>(null)

  // Personal Info of User
  private currentUser = new BehaviorSubject<User | null>(null)

  // Access Token Timer
  private ATTimer?: Subscription
  // Refresh Threehold in 3000 miliseconds or 3 seconds
  private readonly REFRESH_THREEHOLD_MS = 60000

  constructor() {
    this.recoverStates();
  }

  public recoverStates(): void {
    this.isAuthenticated().pipe(first(), 
      filter(a => !a), takeUntil(this.destroy)).subscribe(a => {
      if(!a) {
        const tokens = this.getTokensFromStorage()
        if(tokens != null) {
          this.setAuthTokens(tokens, true)
        }
      }
    })
  }
  
  private isValidTokenDecode(decode: TokenDecode): boolean {
    return decode.exp > decode.iat && !!decode.sub
  }

  public isAuthenticated(): Observable<boolean> {
    return this.decodeState.asObservable().pipe(
      map(a => a !== null),
      distinctUntilChanged(),
      shareReplay({ bufferSize: 1, refCount: true })
    )
  }

  public getJWTState(): Observable<[JWTTokens | null, string | null]> {
    return combineLatest([this.jwtState, this.clientId]).pipe(
      distinctUntilChanged(),
      shareReplay({ bufferSize: 1, refCount: true })
    )
  }

  public getClientID(): Observable<string | null> {
    return this.clientId.asObservable().pipe(
      distinctUntilChanged(),
      shareReplay({ bufferSize: 1, refCount: true }),
    )
  }

  public getUserInfo(): Observable<User | null> {
    return this.currentUser.asObservable().pipe(
      distinctUntilChanged(),
      shareReplay({ bufferSize: 1, refCount: true })
    )
  }

  private getTokensFromStorage(): AuthResponse | null {
    const c = this.storage.get(CLIENT_ID)
    const a = this.storage.get(ACCESS_TOKEN)
    const b = this.storage.get(REFRESH_TOKEN)
    this.manageClientID()
    
    if(a == null || b == null || a === "" || b === "") {
      return null
    }
    return { accessToken: a, refreshToken: b }
  }

  public setAuthTokens(tokens: AuthResponse, isRecovered: boolean): void {
    const AToken = jwtDecode<TokenDecode>(tokens.accessToken)
    const RToken = jwtDecode<TokenDecode>(tokens.refreshToken)
    if(this.isValidTokenDecode(AToken) && this.isValidTokenDecode(RToken)) {
      this.jwtState.next({ access: tokens.accessToken, refresh: tokens.refreshToken })
      this.decodeState.next({ access: AToken, refresh: RToken })

      let now = Date.now()
      let ATExp = new Date(AToken.exp*1000)
      let delay = ATExp.getTime() - now - this.REFRESH_THREEHOLD_MS
      this.ATTimer?.unsubscribe()
      this.ATTimer = timer(Math.max(delay, 0))
        .pipe(takeUntil(this.destroy), switchMap(() => this.rotateTokens())).subscribe()

      if(!isRecovered) {
        this.storage.set(ACCESS_TOKEN, tokens.accessToken, ATExp, "/", undefined, true, "Lax", false)
        this.storage.set(REFRESH_TOKEN, tokens.refreshToken, ATExp, "/", undefined, true, "Lax", false)
      }
      this.manageClientID()
    }
  }

  private cleanStates(): void {
    this.jwtState.next(null)
    this.decodeState.next(null)
    this.currentUser.next(null)
    this.clientId.next(null)
    this.storage.delete(ACCESS_TOKEN, "/", undefined, true, "Lax")
    this.storage.delete(REFRESH_TOKEN, "/", undefined, true, "Lax")
    this.storage.delete(CLIENT_ID, "/", undefined, true, "Lax")
  }

  public logout(): Observable<void> {
    return this.getJWTState().pipe(
      filter(i => i[0] != null && i[1] != null), 
      switchMap((v) => this.authService.logout(v[0]?.refresh || "", v[1] || "")),
      tap({ next: () => this.cleanStates() })
    )
  }

  public rotateTokens() {
    return this.getJWTState().pipe(
      filter(i => i[0] != null && i[1] != null), 
      switchMap((i) => this.authService.rotateTokens(i[0]?.refresh || "", i[1] || "")),
      tap((a) => {
        if(a) {
          this.cleanStates()
          this.setAuthTokens(a, false)
        }
      }),
    )
  }

  public setUserInfo(): Observable<User> {
    return this.getJWTState().pipe(
      filter(i => i[0] != null && i[1] != null), 
      switchMap(i => this.authService.getUserInfo()),
      tap(i => this.currentUser.next(i)),
      catchError(() => EMPTY)
    )
  }

  private manageClientID(): void {
    const c = this.storage.get(CLIENT_ID)
    if(c == null || c == "" || !(validate(c) && version(c) === 7)) {
      this.clientId.next(v7())
      this.storage.set(CLIENT_ID, v7(), undefined, "/", undefined, true, "Lax", false)
    } else {
      this.clientId.next(c)
    }
  }

}
