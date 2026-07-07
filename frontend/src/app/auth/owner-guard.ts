import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthenticationService } from './authentication-service';
import { map } from 'rxjs';

/**
 * Route guard that verifies the authenticated user owns the requested resource.
 * Uses the `id` route parameter and the `resourceType` route data to determine
 * ownership:
 * - `'profile'` — matches against `userInfo.profileId`.
 * - `'advertiser'` — matches against `userInfo.advertiserId`.
 * - `'own'` — always grants access without a resource check.
 *
 * Redirects unauthenticated users to `/login` and non-owners to `/dashboard`.
 * @param route The activated route snapshot containing the `id` param and `resourceType` data.
 * @returns `true` if the user owns the resource, otherwise a `UrlTree` to `/login` or `/dashboard`.
 */
export const ownerGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  const authService = inject(AuthenticationService);
  const resourceId = route.paramMap.get('id');
  const resourceType = route.data['resourceType'];

  return authService.getUserInfo().pipe(
    map((userInfo) => {
      if (!userInfo) {
        return router.createUrlTree(['/login']);
      }

      if (resourceType === 'profile' && userInfo.profileId === resourceId) {
        return true;
      }
      if (resourceType === 'advertiser' && userInfo.advertiserId === resourceId) {
        return true;
      }
      if (resourceType === 'own') {
        return true;
      }

      return router.createUrlTree(['/dashboard']);
    }),
  );
};
