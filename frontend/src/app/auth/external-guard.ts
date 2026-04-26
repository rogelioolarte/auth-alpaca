import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthenticationService } from './authentication-service';
import { of, switchMap } from 'rxjs';

export const externalGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthenticationService);
  return authService
    .isAuthenticated()
    .pipe(switchMap((i) => (!i ? of(true) : of(router.createUrlTree(['/dashboard'])))));
};
