import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from './core/services/auth.service';
import { ComercioService } from './core/services/comercio.service';
import { GenericResponse, TotpConfigResponse } from './core/models/api.models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (authService.isAuthenticated()) {
      <div class="app">
        <nav class="nav" aria-label="Principal">
          <a routerLink="/catalogo" class="brand" aria-label="AppHasani">
            <span class="mark">A</span>
            <span class="brand-name">AppHasani</span>
          </a>
          <a routerLink="/catalogo" routerLinkActive="active" class="link">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></svg>
            <span>Servicios</span>
          </a>
          <a routerLink="/compra" routerLinkActive="active" class="link">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/></svg>
            <span>Pagar</span>
          </a>
          <a routerLink="/historial" routerLinkActive="active" class="link">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
            <span>Historial</span>
          </a>
        </nav>

        <div class="main">
          <header class="topbar">
            <div class="account">
              @if (comercioService.cuentaActiva(); as cta) {
                <div class="balance-block">
                  <span class="small muted">Saldo disponible</span>
                  <strong class="balance num">{{ ocultarSaldo() ? '••••••' : (cta.saldo | currency: 'MXN' : 'symbol-narrow' : '1.2-2') }}</strong>
                </div>
                @if (comercioService.cuentasUsuario().length > 1) {
                  <select class="input acct-select num" aria-label="Cuenta" [value]="cta.numeroCuenta" (change)="onCambiarCuenta($event)">
                    @for (c of comercioService.cuentasUsuario(); track c.id) {
                      <option [value]="c.numeroCuenta">•••• {{ c.numeroCuenta.slice(-4) }}</option>
                    }
                  </select>
                } @else {
                  <span class="chip num">•••• {{ cta.numeroCuenta.slice(-4) }}</span>
                }
              }
            </div>
            <div class="actions">
              <button type="button" class="icon-btn" [attr.aria-label]="ocultarSaldo() ? 'Mostrar saldo' : 'Ocultar saldo'" (click)="ocultarSaldo.set(!ocultarSaldo())">
                @if (ocultarSaldo()) {
                  <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17.9 17.9A10.1 10.1 0 0 1 12 20c-7 0-10-8-10-8a18 18 0 0 1 5.1-5.9M9.9 4.2A9.1 9.1 0 0 1 12 4c7 0 10 8 10 8a18 18 0 0 1-2.2 3.2M1 1l22 22"/></svg>
                } @else {
                  <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                }
              </button>
              <button type="button" class="icon-btn" aria-label="Actualizar saldo" [disabled]="comercioService.cargandoPerfil()" (click)="refrescarSaldo()">
                <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-3-6.7L21 8"/><path d="M21 3v5h-5"/></svg>
              </button>
              <button type="button" class="icon-btn" title="Seguridad 2FA (Google Authenticator)" aria-label="Seguridad 2FA" (click)="abrirModalSeguridad()">
                <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
              </button>
              <button type="button" class="icon-btn" [attr.title]="authService.userEmail()" aria-label="Cerrar sesión" (click)="authService.logout()">
                <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5M21 12H9"/></svg>
              </button>
            </div>
          </header>
          <main><router-outlet></router-outlet></main>
        </div>
      </div>

      @if (modalSeguridad()) {
        <div class="modal-backdrop" role="dialog" aria-modal="true">
          <div class="modal">
            <div class="row-between" style="margin-bottom: 0.75rem;">
              <h2>Seguridad 2FA</h2>
              <button type="button" class="icon-btn" aria-label="Cerrar" (click)="cerrarModalSeguridad()">✕</button>
            </div>
            <p class="muted small" style="margin-bottom: 1rem;">
              Método de verificación:
              <strong style="color: var(--ink);">Google Authenticator</strong>
              ({{ totpHabilitado() ? 'Activo' : 'Pendiente de vincular' }})
            </p>

            @if (totpError()) {
              <div class="alert alert-danger">{{ totpError() }}</div>
            }
            @if (totpOk()) {
              <div class="alert alert-success">{{ totpOk() }}</div>
            }

            @if (totpCargando() && !totpConfig()) {
              <div class="center" style="padding: 1.5rem 0;"><span class="spinner dark"></span></div>
            } @else {
              @if (totpConfig(); as cfg) {
              <div class="seg" role="tablist" style="width: 100%; display: grid; grid-template-columns: 1fr 1fr; margin-bottom: 0.85rem;">
                <button type="button" [class.on]="modoVinculacion() === 'QR'"
                        [disabled]="!cfg.qrBase64" (click)="modoVinculacion.set('QR')">
                  Código QR
                </button>
                <button type="button" [class.on]="modoVinculacion() === 'CLAVE'"
                        (click)="modoVinculacion.set('CLAVE')">
                  Clave manual
                </button>
              </div>

              @if (modoVinculacion() === 'QR' && cfg.qrBase64) {
                <div style="display:flex; flex-direction:column; align-items:center;">
                  <div style="padding:0.6rem; background:#fff; border:1px solid var(--line); border-radius:14px; display:inline-flex;">
                    <img [src]="'data:image/png;base64,' + cfg.qrBase64"
                         alt="Código QR Google Authenticator" width="196" height="196" style="display:block;" />
                  </div>
                  <p class="muted small" style="margin: 0.65rem 0 0; text-align:center;">
                    Abre <strong>Google Authenticator</strong> → <strong>+</strong> → <em>Escanear un código QR</em>.
                  </p>
                </div>
              } @else {
                <ol class="small muted" style="padding-left: 1.15rem; margin: 0 0 0.85rem; display: grid; gap: 0.35rem;">
                  <li>Abre <strong>Google Authenticator</strong> en tu teléfono y toca <strong>+</strong> → <em>Ingresar clave de configuración</em>.</li>
                  <li>Cuenta: <strong>AppHasani ({{ cfg.correo }})</strong> · Tipo: <em>Basado en tiempo</em>.</li>
                  <li>Copia y pega esta clave secreta:</li>
                </ol>
                <div class="secret-box">
                  <code class="num">{{ formatearSecreto(cfg.secreto) }}</code>
                  <button type="button" class="btn btn-secondary" style="min-height: 34px; padding: 0 0.8rem; font-size: 0.8rem;" (click)="copiarSecreto(cfg.secreto)">
                    {{ secretoCopiado() ? 'Copiada' : 'Copiar clave' }}
                  </button>
                </div>
              }

              <div class="field" style="margin-top: 0.9rem;">
                <label for="totpCodigoOn">Código de 6 dígitos de tu app</label>
                <input id="totpCodigoOn" type="text" inputmode="numeric" maxlength="6" class="input num"
                       style="text-align:center; letter-spacing:0.3rem; font-weight:700;"
                       [value]="totpCodigo()" (input)="alCambiarTotpCodigo($event)" placeholder="000000" />
              </div>
              <div class="modal-actions">
                <button type="button" class="btn btn-secondary" [disabled]="totpCargando()" (click)="cerrarModalSeguridad()">Cancelar</button>
                <button type="button" class="btn btn-primary" [disabled]="totpCargando() || totpCodigo().length !== 6" (click)="activarTotp()">
                  @if (totpCargando()) { <span class="spinner"></span> } @else { Activar }
                </button>
              </div>
            } @else {
              <p class="small muted" style="margin-bottom: 1rem;">
                {{ totpHabilitado()
                    ? 'Tu cuenta utiliza Google Authenticator como segundo factor. Si cambiaste de teléfono, puedes generar un nuevo código QR y clave secreta.'
                    : 'Vincula Google Authenticator escaneando el código QR o ingresando la clave secreta.' }}
              </p>
                <div class="modal-actions">
                  <button type="button" class="btn btn-secondary" (click)="cerrarModalSeguridad()">Cerrar</button>
                  <button type="button" class="btn btn-primary" [disabled]="totpCargando()" (click)="iniciarConfiguracionTotp()">
                    @if (totpCargando()) { <span class="spinner"></span> } @else {
                      {{ totpHabilitado() ? 'Reconfigurar Google Authenticator' : 'Configurar Google Authenticator' }}
                    }
                  </button>
                </div>
              }
            }
          </div>
        </div>
      }
    } @else {
      <router-outlet></router-outlet>
    }
  `,
  styles: [`
    .app { display: flex; min-height: 100vh; }
    .nav { width: 200px; flex-shrink: 0; background: var(--surface); border-right: 1px solid var(--line); padding: 1.25rem 0.85rem; display: flex; flex-direction: column; gap: 0.25rem; position: sticky; top: 0; height: 100vh; }
    .brand { display: flex; align-items: center; gap: 0.6rem; padding: 0.25rem 0.5rem 1.25rem; text-decoration: none; color: var(--ink); }
    .mark { width: 30px; height: 30px; border-radius: 8px; background: var(--brand); color: #fff; font-weight: 700; display: inline-flex; align-items: center; justify-content: center; }
    .brand-name { font-weight: 700; letter-spacing: -0.01em; }
    .link { display: flex; align-items: center; gap: 0.7rem; padding: 0.6rem 0.7rem; border-radius: var(--radius-sm); color: var(--ink-2); text-decoration: none; font-weight: 600; font-size: 0.92rem; }
    .link:hover { background: #f1f3f6; color: var(--ink); }
    .link.active { background: var(--brand-soft); color: var(--brand); }
    .main { flex: 1; min-width: 0; }
    .topbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: 0.7rem 1.5rem; background: var(--surface); border-bottom: 1px solid var(--line); position: sticky; top: 0; z-index: 20; }
    .account { display: flex; align-items: center; gap: 0.9rem; min-width: 0; }
    .balance-block { display: flex; flex-direction: column; line-height: 1.2; }
    .balance { font-size: 1.15rem; }
    .chip { padding: 0.2rem 0.6rem; border-radius: 999px; background: #eef0f4; color: var(--ink-2); font-size: 0.82rem; font-weight: 600; }
    .acct-select { width: auto; min-height: 34px; font-size: 0.85rem; padding: 0 0.6rem; }
    .actions { display: flex; align-items: center; gap: 0.15rem; }
    .secret-box { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; padding: 0.7rem 0.85rem; background: var(--brand-soft); border-radius: var(--radius-sm); }
    .secret-box code { font-weight: 700; color: var(--brand); letter-spacing: 0.08rem; word-break: break-all; font-size: 0.85rem; }
    @media (max-width: 720px) {
      .nav { position: fixed; bottom: 0; left: 0; right: 0; top: auto; width: 100%; height: auto; flex-direction: row; justify-content: space-around; border-right: none; border-top: 1px solid var(--line); padding: 0.4rem 0.5rem; z-index: 30; }
      .brand { display: none; }
      .link { flex-direction: column; gap: 0.15rem; font-size: 0.72rem; padding: 0.35rem 1rem; }
      .topbar { padding: 0.6rem 1rem; }
    }
  `]
})
export class AppComponent {
  readonly authService = inject(AuthService);
  readonly comercioService = inject(ComercioService);
  readonly ocultarSaldo = signal(false);

  readonly modalSeguridad = signal(false);
  readonly totpHabilitado = signal(false);
  readonly totpCargando = signal(false);
  readonly totpConfig = signal<TotpConfigResponse | null>(null);
  readonly totpCodigo = signal('');
  readonly modoVinculacion = signal<'QR' | 'CLAVE'>('QR');
  readonly totpError = signal<string | null>(null);
  readonly totpOk = signal<string | null>(null);
  readonly secretoCopiado = signal(false);

  constructor() {
    effect(() => {
      const autenticado = this.authService.isAuthenticated();
      const email = this.authService.userEmail();
      if (autenticado && email) {
        this.comercioService.cargarPerfilYCuentas(email);
      } else {
        this.comercioService.limpiarEstadoUsuario();
      }
    }, { allowSignalWrites: true });
  }

  onCambiarCuenta(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (value) {
      this.comercioService.seleccionarCuentaActiva(value);
    }
  }

  refrescarSaldo(): void {
    const email = this.authService.userEmail();
    if (email) {
      this.comercioService.cargarPerfilYCuentas(email);
    }
  }

  abrirModalSeguridad(): void {
    this.modalSeguridad.set(true);
    this.totpError.set(null);
    this.totpOk.set(null);
    this.totpConfig.set(null);
    this.totpCodigo.set('');
    this.totpCargando.set(true);
    this.authService.obtenerEstadoTotp().subscribe({
      next: (resp) => {
        this.totpCargando.set(false);
        this.totpHabilitado.set(!!resp.datos?.habilitado);
      },
      error: (err: HttpErrorResponse) => {
        this.totpCargando.set(false);
        const body = err.error as GenericResponse<unknown> | null;
        this.totpError.set(body?.mensaje || 'No se pudo consultar el estado 2FA.');
      }
    });
  }

  cerrarModalSeguridad(): void {
    this.modalSeguridad.set(false);
    this.totpConfig.set(null);
    this.totpCodigo.set('');
  }

  iniciarConfiguracionTotp(): void {
    this.totpError.set(null);
    this.totpOk.set(null);
    this.totpCargando.set(true);
    this.authService.configurarTotp().subscribe({
      next: (resp) => {
        this.totpCargando.set(false);
        const cfg = resp.datos ?? null;
        this.totpConfig.set(cfg);
        this.modoVinculacion.set(cfg?.qrBase64 ? 'QR' : 'CLAVE');
      },
      error: (err: HttpErrorResponse) => {
        this.totpCargando.set(false);
        const body = err.error as GenericResponse<unknown> | null;
        this.totpError.set(body?.mensaje || 'No se pudo generar la clave.');
      }
    });
  }

  activarTotp(): void {
    this.totpError.set(null);
    this.totpCargando.set(true);
    this.authService.activarTotp(this.totpCodigo()).subscribe({
      next: () => {
        this.totpCargando.set(false);
        this.totpHabilitado.set(true);
        this.totpConfig.set(null);
        this.totpCodigo.set('');
        this.totpOk.set('Google Authenticator activado correctamente.');
      },
      error: (err: HttpErrorResponse) => {
        this.totpCargando.set(false);
        const body = err.error as GenericResponse<unknown> | null;
        this.totpError.set(body?.mensaje || 'Código incorrecto.');
      }
    });
  }

  alCambiarTotpCodigo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const limpio = input.value.replace(/\D/g, '').slice(0, 6);
    if (input.value !== limpio) input.value = limpio;
    this.totpCodigo.set(limpio);
  }

  formatearSecreto(secreto: string): string {
    return secreto.replace(/(.{4})/g, '$1 ').trim();
  }

  copiarSecreto(secreto: string): void {
    void navigator.clipboard?.writeText(secreto);
    this.secretoCopiado.set(true);
    setTimeout(() => this.secretoCopiado.set(false), 2000);
  }
}
