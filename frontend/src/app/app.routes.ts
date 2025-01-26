import {Routes} from '@angular/router';
import {AuthGuard} from "./auth/auth.guard";
import {Oauth2RedirectHandlerComponent} from "./auth/oauth2-redirect-handler/oauth2-redirect-handler.component";
import {LoginComponent} from "./user/login/login.component";
import {DashboardComponent} from "./user/dashboard/dashboard.component";
import {ProfileComponent} from "./user/profile/profile.component";

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login'},
  { path: 'login', component: LoginComponent },
  { path: 'oauth2/:provider/redirect', component: Oauth2RedirectHandlerComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard],
    children: [
      { path: 'profile/:authProvider', component: ProfileComponent, canActivate: [AuthGuard] }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
