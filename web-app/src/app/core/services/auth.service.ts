import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ClienteResponse,
  DisponibilidadRegistroResponse,
  GenericResponse,
  LoginRequest,
  LoginResponse,
  RegistroClienteRequest,
  TotpConfigResponse,
  TotpEstadoResponse,
  UbicacionCatalogoResponse,
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

  registrarCliente(payload: RegistroClienteRequest): Observable<GenericResponse<ClienteResponse>> {
    return this.http.post<GenericResponse<ClienteResponse>>(`${environment.apiUrl}/clientes`, payload);
  }

  obtenerUbicacionesPorEstado(estado: string): Observable<GenericResponse<UbicacionCatalogoResponse[]>> {
    return this.http.get<GenericResponse<UbicacionCatalogoResponse[]>>(
      `${environment.apiUrl}/catalogos/ubicaciones`,
      { params: { estado } }
    );
  }

  buscarPorCodigoPostal(codigoPostal: string): Observable<GenericResponse<UbicacionCatalogoResponse[]>> {
    return this.http.get<GenericResponse<UbicacionCatalogoResponse[]>>(
      `${environment.apiUrl}/catalogos/cp/${codigoPostal.trim()}`
    );
  }

  obtenerOcupaciones(): Observable<GenericResponse<string[]>> {
    return this.http.get<GenericResponse<string[]>>(`${environment.apiUrl}/catalogos/ocupaciones`);
  }

  obtenerColoniasPorCp(codigoPostal: string): Observable<GenericResponse<string[]>> {
    return this.http.get<GenericResponse<string[]>>(`${environment.apiUrl}/catalogos/colonias`, {
      params: { codigoPostal: codigoPostal.trim() }
    });
  }

  obtenerEstadoTotp(): Observable<GenericResponse<TotpEstadoResponse>> {
    return this.http.get<GenericResponse<TotpEstadoResponse>>(`${environment.apiUrl}/seguridad/totp/estado`);
  }

  configurarTotp(): Observable<GenericResponse<TotpConfigResponse>> {
    return this.http.post<GenericResponse<TotpConfigResponse>>(`${environment.apiUrl}/seguridad/totp/configurar`, {});
  }

  activarTotp(codigo: string): Observable<GenericResponse<TotpEstadoResponse>> {
    return this.http.post<GenericResponse<TotpEstadoResponse>>(`${environment.apiUrl}/seguridad/totp/activar`, {
      codigo: codigo.trim()
    });
  }

  desactivarTotp(password: string): Observable<GenericResponse<TotpEstadoResponse>> {
    return this.http.post<GenericResponse<TotpEstadoResponse>>(`${environment.apiUrl}/seguridad/totp/desactivar`, {
      password
    });
  }

  verificarDisponibilidad(params: {
    curp?: string;
    rfc?: string;
    correo?: string;
    telefonoMovil?: string;
  }): Observable<GenericResponse<DisponibilidadRegistroResponse>> {
    const query: Record<string, string> = {};
    if (params.curp?.trim()) query['curp'] = params.curp.trim().toUpperCase();
    if (params.rfc?.trim()) query['rfc'] = params.rfc.trim().toUpperCase();
    if (params.correo?.trim()) query['correo'] = params.correo.trim().toLowerCase();
    if (params.telefonoMovil?.trim()) query['telefonoMovil'] = params.telefonoMovil.trim();
    return this.http.get<GenericResponse<DisponibilidadRegistroResponse>>(
      `${environment.apiUrl}/clientes/disponibilidad`,
      { params: query }
    );
  }

  enviarCodigoVerificacionCorreo(correo: string): Observable<GenericResponse<{ segundosExpiracion: number }>> {
    return this.http.post<GenericResponse<{ segundosExpiracion: number }>>(
      `${environment.apiUrl}/clientes/verificar-correo/enviar`,
      { correo: correo.trim().toLowerCase() }
    );
  }

  confirmarCodigoVerificacionCorreo(correo: string, codigo: string): Observable<GenericResponse<boolean>> {
    return this.http.post<GenericResponse<boolean>>(
      `${environment.apiUrl}/clientes/verificar-correo/confirmar`,
      { correo: correo.trim().toLowerCase(), codigo: codigo.trim() }
    );
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

