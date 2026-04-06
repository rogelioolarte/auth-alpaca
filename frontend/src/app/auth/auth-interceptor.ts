import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth-service';
import { environment } from '../../environments/environment';
import { EMPTY, switchMap, take } from 'rxjs';
import { ACCESS_TOKEN_HEADER_KEY } from '../models/constants';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService)
  const authRoutes = ['/api/auth','/api/auth']
  const { pathname, host } = new URL(req.url)
  if(authRoutes.some(r => pathname.includes(r)) || host != environment.API_HOST) {
    return next(req);
  }
  return auth.getJWTState().pipe(
    take(1),
    switchMap(v => {
      if(v != null) {
        return next(req.clone({ 
          setHeaders: { [ACCESS_TOKEN_HEADER_KEY]: `Bearer ${v.access}` } 
        }))
      } else {
        return EMPTY
      }
    })
  )
};
