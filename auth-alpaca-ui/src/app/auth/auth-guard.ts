import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthenticationService } from './authentication-service';
import { of, switchMap } from 'rxjs';

/**
 * Route guard that allows access only to authenticated users.
 * Redirects unauthenticated users to the `/login` page.
 * @returns `true` if the user is authenticated, otherwise a `UrlTree` to `/login`.
 */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthenticationService);
  return authService
    .isAuthenticated()
    .pipe(switchMap((i) => (i ? of(true) : of(router.createUrlTree(['/login'])))));
};
