import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'registro',
    loadComponent: () => import('./features/auth/registro.component').then((m) => m.RegistroComponent)
  },
  {
    path: 'catalogo',
    canActivate: [authGuard],
    loadComponent: () => import('./features/catalogo/catalogo.component').then((m) => m.CatalogoComponent)
  },
  {
    path: 'compra',
    canActivate: [authGuard],
    loadComponent: () => import('./features/compra/compra.component').then((m) => m.CompraComponent)
  },
  {
    path: 'historial',
    canActivate: [authGuard],
    loadComponent: () => import('./features/historial/historial.component').then((m) => m.HistorialComponent)
  },
  {
    path: '',
    redirectTo: 'catalogo',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'catalogo'
  }
];

