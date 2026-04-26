import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthenticationService } from './authentication-service';
import { map } from 'rxjs';

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
