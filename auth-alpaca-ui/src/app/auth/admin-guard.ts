import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthenticationService } from './authentication-service';
import { map } from 'rxjs';

/**
 * Route guard that restricts access to users with the ADMIN role.
 * Non-admin users are redirected to the `/dashboard` page.
 * @returns `true` if the user has the ADMIN role, otherwise a `UrlTree` to `/dashboard`.
 */
export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthenticationService);

  return authService.isAdmin().pipe(
    map((hasRole) => {
      if (hasRole) {
        return true;
      }
      return router.createUrlTree(['/dashboard']);
    }),
  );
};
