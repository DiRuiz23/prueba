import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  const esRutaAuth = req.url.includes('/auth/');

  const peticion = token && !req.url.includes('/auth/login') && !req.url.includes('/auth/verificar-2fa')
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      if ((error.status === 401 || (error.status === 403 && !req.url.includes('/pago-servicio'))) && !esRutaAuth) {
        authService.limpiarSesionLocal(
          'Tu sesión ha expirado o fue cerrada porque iniciaste sesión en otro dispositivo.'
        );
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
