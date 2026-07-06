import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthenticationService } from './authentication-service';
import { of, switchMap } from 'rxjs';

/**
 * Route guard that allows access only to unauthenticated users.
 * Typically used for login and registration pages. Authenticated users
 * are redirected to the `/dashboard` page.
 * @returns `true` if the user is not authenticated, otherwise a `UrlTree` to `/dashboard`.
 */
export const externalGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthenticationService);
  return authService
    .isAuthenticated()
    .pipe(switchMap((i) => (!i ? of(true) : of(router.createUrlTree(['/dashboard'])))));
};
