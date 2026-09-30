import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SetupService } from './setup.service';

/** Every screen but setup itself: nothing works until the first Agent exists. */
export const setupDoneGuard: CanActivateFn = async () => {
  const router = inject(Router);
  return (await inject(SetupService).isRequired()) ? router.createUrlTree(['/setup']) : true;
};

/** The setup screen: once someone exists, the way in is the login screen. */
export const setupPendingGuard: CanActivateFn = async () => {
  const router = inject(Router);
  return (await inject(SetupService).isRequired()) ? true : router.createUrlTree(['/login']);
};
