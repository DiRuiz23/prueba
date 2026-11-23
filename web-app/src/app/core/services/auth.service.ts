import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  GenericResponse,
  LoginRequest,
  LoginResponse,
  VerificarOtpRequest
} from '../models/api.models';

const TOKEN_KEY = 'apphasani_jwt_token';
const USER_EMAIL_KEY = 'apphasani_user_email';
const SESSION_REASON_KEY = 'apphasani_session_reason';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly tokenSignal = signal<string | null>(sessionStorage.getItem(TOKEN_KEY));
  private readonly emailSignal = signal<string | null>(sessionStorage.getItem(USER_EMAIL_KEY));
  readonly sessionAlertSignal = signal<string | null>(sessionStorage.getItem(SESSION_REASON_KEY));

  readonly isAuthenticated = computed(() => {
    const token = this.tokenSignal();
    return !!token && !this.esJwtExpirado(token);
  });

  readonly userEmail = computed(() => this.emailSignal());

  getToken(): string | null {
    return this.tokenSignal();
  }

  clearSessionAlert(): void {
    sessionStorage.removeItem(SESSION_REASON_KEY);
    this.sessionAlertSignal.set(null);
  }

  login(correo: string, password: string, forzarCierre = false): Observable<GenericResponse<LoginResponse>> {
    const payload: LoginRequest = {
      correo: correo.trim().toLowerCase(),
      password,
      dispositivo: 'WEB',
      forzarCierreSesionAnterior: forzarCierre
    };
    return this.http.post<GenericResponse<LoginResponse>>(`${this.baseUrl}/login`, payload);
  }

  verificar2fa(correo: string, codigo: string): Observable<GenericResponse<LoginResponse>> {
    const payload: VerificarOtpRequest = {
      correo: correo.trim().toLowerCase(),
      codigo: codigo.trim(),
      dispositivo: 'WEB'
    };
    return this.http.post<GenericResponse<LoginResponse>>(`${this.baseUrl}/verificar-2fa`, payload).pipe(
      tap((resp) => {
        const token = resp.datos?.token;
        const email = resp.datos?.correo;
        if (token && email) {
          this.guardarSesion(token, email);
        }
      })
    );
  }

  forzarCierreSesion(correo: string): Observable<GenericResponse<boolean>> {
    return this.http.post<GenericResponse<boolean>>(`${this.baseUrl}/forzar-cierre-sesion`, {
      correo: correo.trim().toLowerCase()
    });
  }

  logout(motivo?: string): void {
    const token = this.getToken();
    if (token) {
      this.http.post(`${this.baseUrl}/logout`, {}).pipe(catchError(() => of(null))).subscribe();
    }
    this.limpiarSesionLocal(motivo);
    void this.router.navigate(['/login']);
  }

  limpiarSesionLocal(motivo?: string): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_EMAIL_KEY);
    this.tokenSignal.set(null);
    this.emailSignal.set(null);
    if (motivo) {
      sessionStorage.setItem(SESSION_REASON_KEY, motivo);
      this.sessionAlertSignal.set(motivo);
    }
  }

  private guardarSesion(token: string, correo: string): void {
    sessionStorage.setItem(TOKEN_KEY, token);
    sessionStorage.setItem(USER_EMAIL_KEY, correo);
    sessionStorage.removeItem(SESSION_REASON_KEY);
    this.tokenSignal.set(token);
    this.emailSignal.set(correo);
    this.sessionAlertSignal.set(null);
  }

  private esJwtExpirado(token: string): boolean {
    try {
      const partes = token.split('.');
      if (partes.length !== 3) {
        return true;
      }
      const payload = JSON.parse(atob(partes[1].replace(/-/g, '+').replace(/_/g, '/'))) as { exp?: number };
      if (!payload.exp) {
        return false;
      }
      return Date.now() >= payload.exp * 1000;
    } catch {
      return true;
    }
  }
}

