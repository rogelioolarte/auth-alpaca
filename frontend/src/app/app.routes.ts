import {Routes} from '@angular/router';
import { Oauth2RedirectHandler } from './auth/oauth2-redirect-handler/oauth2-redirect-handler';
import { authGuard } from './auth/auth-guard';
import { Dashboard } from './components/pages/dashboard/dashboard';
import { Login } from './components/pages/login/login';
import { Profile } from './components/pages/profile/profile';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login'},
  { path: 'login', component: Login },
  { path: 'oauth2/:provider/redirect', component: Oauth2RedirectHandler },
  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [authGuard],
    children: [
      { path: 'profile/:authProvider', component: Profile, canActivate: [authGuard] }
    ]
  },
  { path: '**', redirectTo: 'login' }
];