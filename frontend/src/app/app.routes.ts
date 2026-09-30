import { Routes } from '@angular/router';
import { loggedInGuard, loggedOutGuard } from './auth/auth.guards';
import { setupDoneGuard, setupPendingGuard } from './setup/setup.guards';
import { Shell } from './shell/shell';

// setupDoneGuard runs first everywhere: on an empty database every screen leads to setup.
// Every screen is lazy: the initial bundle is the shell, the router and i18n; Material's form
// components load with the first screen that uses them.
export const routes: Routes = [
  {
    // lazy: used once per deployment
    path: 'setup',
    loadComponent: () => import('./setup/setup').then((m) => m.Setup),
    canActivate: [setupPendingGuard],
    title: 'servdesk',
  },
  {
    path: 'login',
    loadComponent: () => import('./login/login').then((m) => m.Login),
    canActivate: [setupDoneGuard, loggedOutGuard],
    title: 'servdesk',
  },
  {
    path: '',
    component: Shell,
    canActivate: [setupDoneGuard, loggedInGuard],
    children: [
      {
        path: 'account',
        loadComponent: () => import('./account/account').then((m) => m.Account),
        title: 'servdesk',
      },
      {
        // lazy: dialogs and snack bars stay out of the initial bundle
        path: 'incidents/:id',
        loadComponent: () => import('./ticket/incident-detail').then((m) => m.IncidentDetail),
        title: 'servdesk',
      },
      { path: '', pathMatch: 'full', redirectTo: 'account' },
    ],
  },
  { path: '**', redirectTo: 'account' },
];
