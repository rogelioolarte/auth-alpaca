import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthenticationService } from './authentication-service';
import { environment } from '../../environments/environment';
import { catchError, EMPTY, switchMap, take, throwError } from 'rxjs';
import { ACCESS_TOKEN_HEADER_KEY } from '../models/constants';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthenticationService)
  const authRoutes = ['/api/auth/login','/api/auth/logout']
  const { pathname, host } = new URL(req.url)
  if(authRoutes.some(r => r === pathname) || host != environment.API_HOST) {
    return next(req);
  }
  return auth.getJWTState().pipe(
    take(1),
    switchMap(v => {
      if(v[0] != null) {
        return next(req.clone({ 
          setHeaders: { [ACCESS_TOKEN_HEADER_KEY]: `Bearer ${v[0].access}` } 
        }))
      } else {
        return next(req).pipe(catchError((err: HttpErrorResponse) => {
          if(err.status === 430) {
            auth.logout()
          }
          return throwError(() => err)
        }))
      }
    })
  )
};
