import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthenticationService } from './authentication-service';
import { map } from 'rxjs';

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
