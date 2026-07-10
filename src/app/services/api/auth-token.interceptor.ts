import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from '../auth.service';

export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.url.includes('/auth/login')) {
    return next(request);
  }

  const token = inject(AuthService).getToken();

  if (!token) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    }),
  );
};
