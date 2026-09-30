import { Routes } from '@angular/router';
import { Account } from './account/account';
import { loggedInGuard, loggedOutGuard } from './auth/auth.guards';
import { Login } from './login/login';
import { setupDoneGuard, setupPendingGuard } from './setup/setup.guards';
import { Setup } from './setup/setup';

// setupDoneGuard runs first everywhere: on an empty database every screen leads to setup
export const routes: Routes = [
  { path: 'setup', component: Setup, canActivate: [setupPendingGuard], title: 'servdesk' },
  {
    path: 'login',
    component: Login,
    canActivate: [setupDoneGuard, loggedOutGuard],
    title: 'servdesk',
  },
  {
    path: 'account',
    component: Account,
    canActivate: [setupDoneGuard, loggedInGuard],
    title: 'servdesk',
  },
  { path: '', pathMatch: 'full', redirectTo: 'account' },
  { path: '**', redirectTo: 'account' },
];
