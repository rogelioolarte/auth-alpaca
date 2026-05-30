import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthenticationService } from './authentication-service';
import { environment } from '../../environments/environment';
import { catchError, switchMap, take, throwError } from 'rxjs';
import { ACCESS_TOKEN_HEADER_KEY } from '../models/constants';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthenticationService);
  const authRoutes = ['/api/auth/login', '/api/auth/logout', '/api/auth/exchange'];
  const { pathname, host } = new URL(req.url);
  if (authRoutes.some((r) => r === pathname) || host != environment.API_HOST) {
    return next(req);
  }
  return auth.getJWTState().pipe(
    take(1),
    switchMap((v) => {
      const accessToken = v[0]?.access;
      const clonedReq = accessToken 
        ? req.clone({ setHeaders: { [ACCESS_TOKEN_HEADER_KEY]: `Bearer ${accessToken}` } })
        : req;

      return next(clonedReq).pipe(
        catchError((err: HttpErrorResponse) => {
          if (err.status === 401) {
            return auth.rotateTokens().pipe(
              switchMap(() => 
                auth.getJWTState().pipe(
                  take(1),
                  switchMap(([tokens]) => {
                    const retryReq = req.clone({
                      setHeaders: { [ACCESS_TOKEN_HEADER_KEY]: `Bearer ${tokens?.access}` },
                    });
                    return next(retryReq);
                  })
                )
              )
            );
          }
          if (err.status === 430) {
            auth.logout();
          }
          return throwError(() => err);
        }),
      );
    }),
  );
};

