import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthenticationService } from './authentication-service';
import { environment } from '../../environments/environment';
import { catchError, first, switchMap, take, throwError } from 'rxjs';
import { ACCESS_TOKEN_HEADER_KEY } from '../models/constants';

/**
 * HTTP interceptor that attaches a Bearer access token to API requests.
 *
 * Skips requests to auth routes (`/api/auth/login`, `/api/auth/exchange`) and
 * external hosts not matching the configured API host. When a 401 response is
 * received (except on the `/api/auth/rotate` endpoint), attempts transparent
 * token rotation and retries the original request once with the new access
 * token. A 430 status triggers an immediate forced logout via
 * {@link AuthenticationService#logout}.
 *
 * @param req - The outgoing HTTP request.
 * @param next - The next handler in the interceptor chain.
 * @returns An observable of the HTTP event stream.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthenticationService);
  const authRoutes = ['/api/auth/login', '/api/auth/exchange'];
  const { pathname, host: host_url } = new URL(req.url);
  const { host: host_api } = new URL(environment.API_URL);
  if (authRoutes.some((r) => r === pathname) || host_url != host_api) {
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
          if (err.status === 401 && pathname != "/api/auth/rotate") {
            return auth.rotateTokens().pipe(
              take(1),
              first(),
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

