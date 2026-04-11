import { ApplicationConfig, InjectionToken, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideToastr } from 'ngx-toastr';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './auth/auth-interceptor';

export const LOCAL_STORAGE = new InjectionToken<Storage>('Local Storage', {
  providedIn: 'root',
  factory: () => window.localStorage
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideToastr(),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor]))
  ]
};
