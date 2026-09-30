import { Routes } from '@angular/router';
import { Account } from './account/account';
import { loggedInGuard, loggedOutGuard } from './auth/auth.guards';
import { Login } from './login/login';

export const routes: Routes = [
  { path: 'login', component: Login, canActivate: [loggedOutGuard], title: 'servdesk' },
  { path: 'account', component: Account, canActivate: [loggedInGuard], title: 'servdesk' },
  { path: '', pathMatch: 'full', redirectTo: 'account' },
  { path: '**', redirectTo: 'account' },
];
