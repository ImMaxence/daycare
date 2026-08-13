import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from '../service/auth.interceptor';
import { environment } from '../environments/env.dev';
import { BASE_PATH } from '../service/api';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),

    provideHttpClient(
      withInterceptors([authInterceptor])
    ),
    { provide: BASE_PATH, useValue: environment.apiUrl }
  ]
};