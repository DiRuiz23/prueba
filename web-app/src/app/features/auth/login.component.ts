import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="wrap">
      <div class="box">
        <div class="logo"><span class="mark">A</span><strong>AppHasani</strong></div>

        <h1>{{ paso() === 'CREDENTIALS' ? 'Iniciar sesión' : 'Código de verificación' }}</h1>
        @if (paso() === 'OTP') {
          <p class="muted small sent">{{ correoActual() }}</p>
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
              <label for="correo">Correo electrónico</label>
              <input id="correo" type="email" class="input" formControlName="correo" autocomplete="email"
                     [class.invalid]="loginForm.controls.correo.touched && loginForm.controls.correo.invalid" />
              @if (loginForm.controls.correo.touched && loginForm.controls.correo.invalid) {
                <span class="field-error">Correo no válido</span>
              }
            </div>
            <div class="field">
              <label for="password">Contraseña</label>
              <div class="pw">
                <input id="password" [type]="mostrarPassword() ? 'text' : 'password'" class="input" formControlName="password" autocomplete="current-password" />
                <button type="button" class="toggle" (click)="mostrarPassword.set(!mostrarPassword())">
                  {{ mostrarPassword() ? 'Ocultar' : 'Mostrar' }}
                </button>
              </div>
              @if (loginForm.controls.password.touched && loginForm.controls.password.invalid) {
                <span class="field-error">Campo obligatorio</span>
              }
            </div>
            <button type="submit" class="btn btn-primary btn-block" [disabled]="cargando() || loginForm.invalid">
              @if (cargando()) { <span class="spinner"></span> } @else { Continuar }
            </button>
          </form>
        } @else {
          <form [formGroup]="otpForm" (ngSubmit)="onSubmitOtp()">
            <div class="field">
              <label for="codigo">Código de 6 dígitos</label>
              <input id="codigo" type="text" inputmode="numeric" maxlength="6" class="input otp num" formControlName="codigo"
                     autocomplete="one-time-code" placeholder="000000" />
              <div class="timer">
                <div class="bar"><div class="fill" [style.width.%]="porcentajeTiempo()"></div></div>
                <span class="small num" [class.low]="segundosRestantes() <= 30">{{ formatoTiempo(segundosRestantes()) }}</span>
              </div>
            </div>
            <button type="submit" class="btn btn-primary btn-block" [disabled]="cargando() || otpForm.invalid || segundosRestantes() <= 0">
              @if (cargando()) { <span class="spinner"></span> } @else { Verificar }
            </button>
            <div class="row-between secondary">
              <button type="button" class="btn btn-secondary" [disabled]="cargando()" (click)="volverACredenciales()">Volver</button>
              <button type="button" class="btn btn-secondary" [disabled]="cargando()" (click)="reenviarOtp()">Reenviar código</button>
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
    .wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 1.5rem; }
    .box { width: 100%; max-width: 400px; background: var(--surface); border: 1px solid var(--line); border-radius: 16px; padding: 2rem; }
    .logo { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 1.75rem; }
    .mark { width: 30px; height: 30px; border-radius: 8px; background: var(--brand); color: #fff; font-weight: 700; display: inline-flex; align-items: center; justify-content: center; }
    h1 { margin-bottom: 1.25rem; }
    .sent { margin: -0.75rem 0 1.25rem; }
    .pw { position: relative; }
    .pw .input { padding-right: 4.5rem; }
    .toggle { position: absolute; right: 0.5rem; top: 50%; transform: translateY(-50%); background: none; border: none; color: var(--brand); font: inherit; font-size: 0.82rem; font-weight: 600; cursor: pointer; }
    .otp { text-align: center; font-size: 1.5rem; letter-spacing: 0.5rem; font-weight: 600; min-height: 52px; }
    .timer { display: flex; align-items: center; gap: 0.6rem; }
    .bar { flex: 1; height: 4px; background: var(--line); border-radius: 99px; overflow: hidden; }
    .fill { height: 100%; background: var(--brand); transition: width 0.5s linear; }
    .low { color: var(--danger); }
    .secondary { margin-top: 0.75rem; }
  `]
})
export class LoginComponent implements OnDestroy {
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
  readonly segundosTotales = signal(300);
  readonly segundosRestantes = signal(300);

  readonly porcentajeTiempo = computed(() => {
    const total = this.segundosTotales();
    return total <= 0 ? 0 : Math.max(0, Math.min(100, (this.segundosRestantes() / total) * 100));
  });

  private timerId: ReturnType<typeof setInterval> | null = null;

  readonly loginForm = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4)]]
  });

  readonly otpForm = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });

  ngOnDestroy(): void {
    this.detenerTemporizador();
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
          this.paso.set('OTP');
          const segs = datos.tiempoExpiracionOtpSegundos ?? 300;
          this.segundosTotales.set(segs);
          this.iniciarTemporizador(segs);
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
          this.detenerTemporizador();
          void this.router.navigate(['/catalogo']);
        } else {
          this.errorMsg.set(resp.mensaje || 'No se recibió el token de sesión.');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorMsg.set(this.extraerMensajeError(err, 'Código inválido o expirado.'));
      }
    });
  }

  reenviarOtp(): void {
    this.otpForm.reset();
    this.onSubmitLogin(true);
  }

  volverACredenciales(): void {
    this.detenerTemporizador();
    this.paso.set('CREDENTIALS');
    this.otpForm.reset();
    this.errorMsg.set(null);
  }

  formatoTiempo(seg: number): string {
    const mins = Math.floor(seg / 60);
    const secs = seg % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  private iniciarTemporizador(segundos: number): void {
    this.detenerTemporizador();
    this.segundosRestantes.set(segundos);
    this.timerId = setInterval(() => {
      const actual = this.segundosRestantes();
      if (actual <= 1) {
        this.segundosRestantes.set(0);
        this.detenerTemporizador();
      } else {
        this.segundosRestantes.set(actual - 1);
      }
    }, 1000);
  }

  private detenerTemporizador(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  private extraerMensajeError(err: HttpErrorResponse, fallback: string): string {
    const body = err.error as { mensaje?: string } | null;
    return body?.mensaje || fallback;
  }
}
