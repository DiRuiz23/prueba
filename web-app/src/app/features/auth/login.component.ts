import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrap">
      <div class="auth-card box">
        <div class="logo"><span class="mark">A</span><strong>AppHasani</strong></div>

        <h1>{{ paso() === 'CREDENTIALS' ? 'Iniciar sesión' : 'Google Authenticator' }}</h1>
        @if (paso() === 'CREDENTIALS') {
          <p class="muted small sub-lead">
            ¿No tienes cuenta? <a routerLink="/registro" class="link-brand">Crear cuenta</a>
          </p>
        } @else {
          <p class="muted small sub-lead">
            Google Authenticator · {{ correoActual() }}
          </p>
        }

        @if (authService.sessionAlertSignal()) {
          <div class="alert alert-warning" role="alert">{{ authService.sessionAlertSignal() }}</div>
        }
        @if (errorMsg()) {
          <div class="alert alert-danger" role="alert">{{ errorMsg() }}</div>
        }

        @if (paso() === 'CREDENTIALS') {
          <form [formGroup]="loginForm" (ngSubmit)="onSubmitLogin()">
            <div class="field">
              <div class="pill-input-wrap">
                <svg class="lead-ic" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/></svg>
                <input id="correo" type="email" maxlength="100" class="pill-input has-icon" placeholder="Correo electrónico" formControlName="correo" autocomplete="email"
                       (input)="sanitizarCorreo()"
                       [class.invalid]="loginForm.controls.correo.touched && loginForm.controls.correo.invalid" />
              </div>
              @if (loginForm.controls.correo.touched && loginForm.controls.correo.invalid) {
                <span class="field-error">Correo no válido</span>
              }
            </div>
            <div class="field">
              <div class="pw pill-input-wrap">
                <svg class="lead-ic" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                <input id="password" maxlength="64" [type]="mostrarPassword() ? 'text' : 'password'" class="pill-input has-icon" placeholder="Contraseña" formControlName="password" autocomplete="current-password" />
                <button type="button" class="toggle" (click)="mostrarPassword.set(!mostrarPassword())">
                  {{ mostrarPassword() ? 'Ocultar' : 'Mostrar' }}
                </button>
              </div>
              @if (loginForm.controls.password.touched && loginForm.controls.password.invalid) {
                <span class="field-error">Campo obligatorio</span>
              }
            </div>
            <div class="cta-end">
              <button type="submit" class="btn btn-primary btn-pill-lg" [disabled]="cargando() || loginForm.invalid">
                @if (cargando()) { <span class="spinner"></span> } @else {
                  <span>Continuar</span>
                  <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
                }
              </button>
            </div>
          </form>
        } @else {
          <form [formGroup]="otpForm" (ngSubmit)="onSubmitOtp()">
            @if (requiereConfiguracionTotp() && totpSecreto()) {
              <div class="totp-setup">
                <div class="seg" role="tablist" aria-label="Método de vinculación">
                  <button type="button" [class.on]="modoVinculacion() === 'QR'"
                          [disabled]="!totpQrBase64()" (click)="modoVinculacion.set('QR')">
                    Código QR
                  </button>
                  <button type="button" [class.on]="modoVinculacion() === 'CLAVE'"
                          (click)="modoVinculacion.set('CLAVE')">
                    Clave manual
                  </button>
                </div>

                @if (modoVinculacion() === 'QR' && totpQrBase64()) {
                  <div class="qr-wrap">
                    <div class="qr-box">
                      <img [src]="'data:image/png;base64,' + totpQrBase64()"
                           alt="Código QR Google Authenticator" width="196" height="196" />
                    </div>
                    <p class="muted small" style="margin: 0.65rem 0 0; text-align: center;">
                      Abre <strong>Google Authenticator</strong> → <strong>+</strong> → <em>Escanear un código QR</em>.
                    </p>
                  </div>
                } @else {
                  <ol class="small muted setup-steps">
                    <li>Abre <strong>Google Authenticator</strong> → <strong>+</strong> → <em>Ingresar clave de configuración</em>.</li>
                    <li>Cuenta: <strong>AppHasani ({{ correoActual() }})</strong> · Tipo: <em>Basado en tiempo</em>.</li>
                  </ol>
                  <div class="secret-box">
                    <code class="num">{{ formatearSecreto(totpSecreto()) }}</code>
                    <button type="button" class="btn btn-secondary copy-btn" (click)="copiarSecreto()">
                      {{ secretoCopiado() ? 'Copiada' : 'Copiar clave' }}
                    </button>
                  </div>
                }
              </div>
            }

            <div class="field">
              <label for="codigo">Código de Google Authenticator (6 dígitos)</label>
              <input id="codigo" type="text" inputmode="numeric" maxlength="6" class="pill-input otp num" formControlName="codigo"
                     (input)="sanitizarOtp()"
                     autocomplete="one-time-code" placeholder="000000" />
              <p class="muted small" style="margin-top: 0.45rem;">Los códigos se renuevan cada 30 s en tu app.</p>
            </div>
            <div class="row-between secondary">
              <button type="button" class="btn btn-secondary" [disabled]="cargando()" (click)="volverACredenciales()">Volver</button>
              <button type="submit" class="btn btn-primary"
                      [disabled]="cargando() || otpForm.invalid">
                @if (cargando()) { <span class="spinner"></span> } @else { Verificar }
              </button>
            </div>
          </form>
        }
      </div>
    </div>

    @if (conflictoSesion()) {
      <div class="modal-backdrop" role="dialog" aria-modal="true">
        <div class="modal">
          <h2>Sesión activa en {{ dispositivoActivo() }}</h2>
          <p class="muted">Solo se permite una sesión a la vez. Si continúas, la sesión anterior se cerrará.</p>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" [disabled]="cargando()" (click)="cancelarConflicto()">Cancelar</button>
            <button type="button" class="btn btn-danger" [disabled]="cargando()" (click)="forzarAcceso()">
              @if (cargando()) { <span class="spinner"></span> } @else { Cerrar la otra sesión }
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .box { max-width: 430px; }
    .logo { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 1.75rem; }
    .mark { width: 32px; height: 32px; border-radius: 10px; background: var(--brand); color: #fff; font-weight: 800; display: inline-flex; align-items: center; justify-content: center; }
    h1 { font-size: 1.75rem; font-weight: 800; margin-bottom: 0.35rem; }
    .sub-lead { margin-bottom: 1.5rem; }
    .pw .pill-input { padding-right: 4.5rem; }
    .toggle { position: absolute; right: 1rem; top: 50%; transform: translateY(-50%); background: none; border: none; color: var(--brand); font: inherit; font-size: 0.82rem; font-weight: 600; cursor: pointer; }
    .btn-pill-lg { min-height: 48px; padding: 0 1.65rem; }
    .totp-setup { margin-bottom: 1.15rem; padding: 0.9rem; border: 1px solid var(--line); border-radius: var(--radius); background: #f8fafc; }
    .totp-setup .seg { width: 100%; display: grid; grid-template-columns: 1fr 1fr; margin-bottom: 0.85rem; }
    .qr-wrap { display: flex; flex-direction: column; align-items: center; }
    .qr-box { padding: 0.6rem; background: #fff; border: 1px solid var(--line); border-radius: 14px; display: inline-flex; }
    .qr-box img { display: block; border-radius: 6px; }
    .setup-steps { padding-left: 1.15rem; margin: 0 0 0.75rem; display: grid; gap: 0.35rem; }
    .secret-box { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; padding: 0.7rem 0.85rem; background: var(--brand-soft); border-radius: var(--radius-sm); }
    .secret-box code { font-weight: 700; color: var(--brand); letter-spacing: 0.08rem; word-break: break-all; font-size: 0.85rem; }
    .copy-btn { min-height: 34px; padding: 0 0.8rem; font-size: 0.8rem; flex-shrink: 0; }
    .otp { text-align: center; font-size: 1.5rem; letter-spacing: 0.5rem; font-weight: 600; min-height: 54px; }
    .secondary { margin-top: 1.25rem; }
    .link-brand { color: var(--brand); font-weight: 700; text-decoration: none; }
    .link-brand:hover { text-decoration: underline; }
  `]
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);

  readonly paso = signal<'CREDENTIALS' | 'OTP'>('CREDENTIALS');
  readonly cargando = signal(false);
  readonly mostrarPassword = signal(false);
  readonly errorMsg = signal<string | null>(null);
  readonly correoActual = signal('');
  readonly conflictoSesion = signal(false);
  readonly dispositivoActivo = signal('MOVIL');
  readonly requiereConfiguracionTotp = signal(false);
  readonly totpSecreto = signal('');
  readonly totpQrBase64 = signal('');
  readonly modoVinculacion = signal<'QR' | 'CLAVE'>('QR');
  readonly secretoCopiado = signal(false);

  readonly loginForm = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4)]]
  });

  readonly otpForm = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });

  sanitizarCorreo(): void {
    const ctrl = this.loginForm.controls.correo;
    const limpio = ctrl.value.replace(/\s/g, '').slice(0, 100);
    if (ctrl.value !== limpio) ctrl.setValue(limpio);
  }

  sanitizarOtp(): void {
    const ctrl = this.otpForm.controls.codigo;
    const limpio = ctrl.value.replace(/\D/g, '').slice(0, 6);
    if (ctrl.value !== limpio) ctrl.setValue(limpio);
  }

  onSubmitLogin(forzarCierre = false): void {
    if (this.loginForm.invalid || this.cargando()) {
      return;
    }
    this.authService.clearSessionAlert();
    this.errorMsg.set(null);
    this.cargando.set(true);

    const { correo, password } = this.loginForm.getRawValue();
    this.authService.login(correo, password, forzarCierre).subscribe({
      next: (resp) => {
        this.cargando.set(false);
        const datos = resp.datos;
        if (!datos) {
          this.errorMsg.set(resp.mensaje || 'Respuesta inesperada del servidor.');
          return;
        }
        this.correoActual.set(datos.correo);

        if (datos.sesionActivaDetectada) {
          this.dispositivoActivo.set(datos.dispositivoSesionActiva || 'otro dispositivo');
          this.conflictoSesion.set(true);
          return;
        }
        if (datos.requiere2fa) {
          this.conflictoSesion.set(false);
          this.requiereConfiguracionTotp.set(!!datos.requiereConfiguracionTotp);
          this.totpSecreto.set(datos.totpSecreto ?? '');
          this.totpQrBase64.set(datos.totpQrBase64 ?? '');
          this.modoVinculacion.set(datos.totpQrBase64 ? 'QR' : 'CLAVE');
          this.secretoCopiado.set(false);
          this.paso.set('OTP');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.conflictoSesion.set(false);
        this.errorMsg.set(this.extraerMensajeError(err, 'No se pudo iniciar sesión.'));
      }
    });
  }

  forzarAcceso(): void {
    this.onSubmitLogin(true);
  }

  cancelarConflicto(): void {
    this.conflictoSesion.set(false);
  }

  onSubmitOtp(): void {
    if (this.otpForm.invalid || this.cargando()) {
      return;
    }
    this.errorMsg.set(null);
    this.cargando.set(true);

    const { codigo } = this.otpForm.getRawValue();
    this.authService.verificar2fa(this.correoActual(), codigo).subscribe({
      next: (resp) => {
        this.cargando.set(false);
        if (resp.datos?.token) {
          void this.router.navigate(['/catalogo']);
        } else {
          this.errorMsg.set(resp.mensaje || 'No se recibió el token de sesión.');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorMsg.set(this.extraerMensajeError(err, 'Código inválido o incorrecto.'));
      }
    });
  }

  volverACredenciales(): void {
    this.paso.set('CREDENTIALS');
    this.requiereConfiguracionTotp.set(false);
    this.totpSecreto.set('');
    this.totpQrBase64.set('');
    this.otpForm.reset();
    this.errorMsg.set(null);
  }

  formatearSecreto(secreto: string): string {
    return secreto.replace(/(.{4})/g, '$1 ').trim();
  }

  copiarSecreto(): void {
    const secreto = this.totpSecreto();
    if (!secreto) return;
    void navigator.clipboard?.writeText(secreto);
    this.secretoCopiado.set(true);
    setTimeout(() => this.secretoCopiado.set(false), 2000);
  }

  private extraerMensajeError(err: HttpErrorResponse, fallback: string): string {
    const body = err.error as { mensaje?: string } | null;
    return body?.mensaje || fallback;
  }
}
