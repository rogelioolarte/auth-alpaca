import { inject, Injectable } from '@angular/core';
import { User } from '../models/user';
import { AuthResponse } from '../models/auth';
import { ACCESS_TOKEN, CLIENT_ID, REFRESH_TOKEN } from '../models/constants';
import { CookieService } from 'ngx-cookie-service'
import { BehaviorSubject, distinctUntilChanged, filter, first, map, Observable, shareReplay, tap } from 'rxjs';
import { DecodeTokens, JWTTokens, TokenDecode } from '../models/decode';
import { jwtDecode } from 'jwt-decode'
import { v7, validate, version } from 'uuid'

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private storage = inject(CookieService)

  // Useful for Interceptor
  private jwtState = new BehaviorSubject<JWTTokens | null>(null)
  // Usefull to check expiration of JWTs
  private decodeState = new BehaviorSubject<DecodeTokens | null>(null)

  private clientId = new BehaviorSubject<string | null>(null)

  // Personal Info of User
  private currentUser = new BehaviorSubject<User | null>(null)

  constructor() {
    this.recoverStates();
  }

  private recoverStates(): void {
    this.isAuthenticated().pipe(filter(a => !a)).subscribe(a => {
      if(!a) {
        const tokens = this.getTokens()
        if(tokens != null) {
          let AToken = jwtDecode<TokenDecode>(tokens.accessToken)
          let RToken = jwtDecode<TokenDecode>(tokens.refreshToken)
          if(this.isValidTokenDecode(AToken) && this.isValidTokenDecode(RToken)) {
            this.jwtState.next({ access: tokens.accessToken, refresh: tokens.refreshToken })
            this.decodeState.next({ access: AToken, refresh: RToken })
          }
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

  public getJWTState(): Observable<JWTTokens | null>  {
    return this.jwtState.asObservable().pipe(
      distinctUntilChanged(),
      shareReplay({ bufferSize: 1, refCount: true }),
    )
  }

  private getTokens(): AuthResponse | null {
    const c = this.storage.get(CLIENT_ID)
    const a = this.storage.get(ACCESS_TOKEN)
    const b = this.storage.get(REFRESH_TOKEN)

    if(c == null || c == "" || !(validate(c) && version(c) === 7)) {
      this.clientId.next(v7())
    } else {
      this.clientId.next(c)
    }
    if(a == null || b == null || a === "" || b === "") {
      return null
    }
    return { accessToken: a, refreshToken: b }
  }

  private setAthentication(tokens: AuthResponse): void {
    this.storage.set(ACCESS_TOKEN, tokens.accessToken)
    this.storage.set(REFRESH_TOKEN, tokens.refreshToken)
  }

  private cleanStates(): void {
    this.jwtState.next(null)
    this.decodeState.next(null)
    this.currentUser.next(null)
    this.clientId.next(null)
    this.storage.delete(ACCESS_TOKEN)
    this.storage.delete(REFRESH_TOKEN)
  }

  public logout(): Observable<boolean> {
    return this.isAuthenticated().pipe(
      first(), 
      tap({ next: () => this.cleanStates() })
    )
  }

}
