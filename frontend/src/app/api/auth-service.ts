import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { REFRESH_TOKEN_HEADER_KEY, CLIENT_ID_HEADER_KEY } from '../models/constants';
import { AuthRequest, AuthResponse } from '../models/auth';
import { Observable } from 'rxjs';
import { User } from '../models/user';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient)

  login(request: AuthRequest):Observable<AuthResponse>  {
    return this.http.post<AuthResponse>(`${environment.API_URL}/auth/login`, request)
  }

  logout(refreshToken: string, clientId: string): Observable<void> {
    return this.http.post<void>(`${environment.API_URL}/auth/logout`, null, 
      { headers: new HttpHeaders({ 
          [REFRESH_TOKEN_HEADER_KEY]: refreshToken, [CLIENT_ID_HEADER_KEY]: clientId
        })
      })
  }

  getUserInfo(): Observable<User> {
    return this.http.get<User>(`${environment.API_URL}/auth/me`)
  }
}
