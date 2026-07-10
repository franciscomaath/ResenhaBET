import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { authTokenInterceptor } from './services/api/auth-token.interceptor';
import { apiErrorToastInterceptor } from './services/api/api-error-toast.interceptor';
import { API_BASE_URL } from './services/api/api-base-url';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authTokenInterceptor, apiErrorToastInterceptor])),
    // { provide: API_BASE_URL, useValue: 'http://localhost:8080/api/v1' },
    { provide: API_BASE_URL, useValue: 'https://resenhabet.duckdns.org/api/v1' },
  ]
};
