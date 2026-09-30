import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * A 401 from any API call other than the two that expect one (a failed login, the startup
 * `/api/me` probe) means the session is gone: back to the login screen.
 */
export const sessionExpiryInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        request.url !== '/api/login' &&
        request.url !== '/api/me'
      ) {
        auth.sessionEnded();
        void router.navigateByUrl('/login');
      }
      return throwError(() => error);
    }),
  );
};
