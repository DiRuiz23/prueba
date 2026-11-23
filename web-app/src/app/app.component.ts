import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { ComercioService } from './core/services/comercio.service';

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
              <button type="button" class="icon-btn" [attr.title]="authService.userEmail()" aria-label="Cerrar sesión" (click)="authService.logout()">
                <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5M21 12H9"/></svg>
              </button>
            </div>
          </header>
          <main><router-outlet></router-outlet></main>
        </div>
      </div>
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
}
