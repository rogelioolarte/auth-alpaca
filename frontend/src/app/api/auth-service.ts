import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { REFRESH_TOKEN_HEADER_KEY, CLIENT_ID_HEADER_KEY } from '../models/constants';
import { AuthRequest, AuthResponse, UserAuth } from '../models/auth';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient)

  login(request: AuthRequest, cliendID: string):Observable<AuthResponse>  {
    return this.http.post<AuthResponse>(`${environment.API_URL}/api/auth/login`, request, { 
      headers: new HttpHeaders({ [CLIENT_ID_HEADER_KEY]: cliendID })
     })
  }

  logout(refreshToken: string, clientId: string): Observable<void> {
    return this.http.post<void>(`${environment.API_URL}/api/auth/logout`, null, 
      { headers: new HttpHeaders({ 
          [REFRESH_TOKEN_HEADER_KEY]: refreshToken, [CLIENT_ID_HEADER_KEY]: clientId
        })
      })
  }

  rotateTokens(refreshToken: string, clientId: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.API_URL}/api/auth/rotate`, null, { 
      headers : new HttpHeaders({
        [REFRESH_TOKEN_HEADER_KEY]: refreshToken,
        [CLIENT_ID_HEADER_KEY]: clientId
      })
    })
  }

  exchangeCode(code : string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.API_URL}/api/auth/exchange`, { code })
  }

  getUserInfo(): Observable<UserAuth> {
    return this.http.get<UserAuth>(`${environment.API_URL}/api/auth/me`)
  }
}
