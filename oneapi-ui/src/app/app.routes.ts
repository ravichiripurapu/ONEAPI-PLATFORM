import { Routes } from '@angular/router';
import { authGuard, adminGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then(m => m.Login)
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./features/admin/dashboard/dashboard').then(m => m.Dashboard)
  },
  {
    path: 'client',
    canActivate: [authGuard],
    loadComponent: () => import('./features/client/dashboard/dashboard').then(m => m.Dashboard)
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
