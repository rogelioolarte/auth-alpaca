import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth-service';
import { of, switchMap } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router)
  const authService = inject(AuthService)
  return authService.isAuthenticated().pipe(
    switchMap(i => i ? of(true) : of(router.createUrlTree(['/login'])))
  );
};
