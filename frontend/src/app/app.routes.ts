import { Routes } from '@angular/router';
import { Oauth2RedirectHandler } from './auth/oauth2-redirect-handler/oauth2-redirect-handler';
import { Login } from './components/pages/login/login';
import { Register } from './components/pages/register/register';
import { Landing } from './components/pages/landing/landing';
import { authGuard } from './auth/auth-guard';
import { adminGuard } from './auth/admin-guard';
import { MyProfile } from './components/pages/my-profile/my-profile';
import { MyAdvertiser } from './components/pages/my-advertiser/my-advertiser';
import { Roles } from './components/pages/roles/roles';
import { Permissions } from './components/pages/permissions/permissions';
import { AdvertiserPage } from './components/pages/advertisers/advertisers';
import { AdvertiserId } from './components/pages/advertiser-id/advertiser-id';
import { Users } from './components/pages/users/users';
import { Base } from './components/pure/base/base';
import { externalGuard } from './auth/external-guard';
import { Dashboard } from './components/pages/dashboard/dashboard';

export const routes: Routes = [
  {
    path: '',
    component: Base,
    children: [
      { path: '', component: Landing },
      { path: 'login', component: Login, canActivate: [externalGuard] },
      { path: 'register', component: Register, canActivate: [externalGuard] },
      { path: 'oauth2/:provider/redirect', component: Oauth2RedirectHandler },
      {
        path: 'advertisers',
        children: [
          { path: '', component: AdvertiserPage },
          { path: ':id', component: AdvertiserId },
        ],
      },

      // Dashboard (authenticated)
      {
        path: 'dashboard',
        canActivate: [authGuard],
        children: [
          // Default child
          { path: '', component: Dashboard, pathMatch: 'full' },

          // User routes
          { path: 'profile', component: MyProfile },
          { path: 'advertiser', component: MyAdvertiser },

          // Admin routes (admin only)
          { path: 'users', component: Users, canActivate: [adminGuard] },
          { path: 'roles', component: Roles, canActivate: [adminGuard] },
          { path: 'permissions', component: Permissions, canActivate: [adminGuard] },
        ],
      },
    ],
  },
  // Redirect unknown routes to landing
  { path: '**', redirectTo: '' },
];
