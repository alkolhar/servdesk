import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Screens that need a logged-in person; everyone else goes to the login screen. */
export const loggedInGuard: CanActivateFn = async () => {
  const router = inject(Router);
  return (await inject(AuthService).currentUser()) ? true : router.createUrlTree(['/login']);
};

/** The login screen itself: someone already logged in has no business there. */
export const loggedOutGuard: CanActivateFn = async () => {
  const router = inject(Router);
  return (await inject(AuthService).currentUser()) ? router.createUrlTree(['/account']) : true;
};
