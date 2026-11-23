import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TimeoutError, timeout } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ComercioService } from '../../core/services/comercio.service';
import { PagoServicioResponse, ProductoItem } from '../../core/models/api.models';

type EstadoResultado = 'NONE' | 'EXITOSA' | 'PENDIENTE' | 'RECHAZADA' | 'ERROR_RED';

@Component({
  selector: 'app-compra',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="page">
      <div class="page-head">
        <h1>Pagar</h1>
      </div>

      @if (!producto()) {
        <div class="surface center">
          <span class="muted">Selecciona un servicio</span>
          <a routerLink="/catalogo" class="btn btn-primary">Ver servicios</a>
        </div>
      } @else {
        <div class="split">
          <form class="surface pad" [formGroup]="compraForm" (ngSubmit)="abrirConfirmacion()">
            <div class="svc">
              <div class="grow">
                <h2>{{ producto()!.producto }}</h2>
                <span class="muted small">{{ producto()!.servicio }}</span>
              </div>
              <a routerLink="/catalogo" class="btn btn-secondary">Cambiar</a>
            </div>

            @if (producto()!.legend) {
              <div class="alert alert-info">{{ producto()!.legend }}</div>
            }

            @if (comercioService.cuentasUsuario().length > 1) {
              <div class="field">
                <label for="numeroCuenta">Cuenta de cargo</label>
                <select id="numeroCuenta" class="input num" formControlName="numeroCuenta" (change)="alCambiarCuenta()">
                  @for (cta of comercioService.cuentasUsuario(); track cta.id) {
                    <option [value]="cta.numeroCuenta">•••• {{ cta.numeroCuenta.slice(-4) }} · {{ cta.saldo | currency: 'MXN' : 'symbol-narrow' : '1.2-2' }}</option>
                  }
                </select>
              </div>
            } @else if (comercioService.cuentasUsuario().length === 0) {
              <div class="field">
                <label for="numeroCuenta">Número de cuenta</label>
                <input id="numeroCuenta" type="text" class="input num" formControlName="numeroCuenta" />
              </div>
            }

            <div class="field">
              <label for="referencia">{{ producto()!.tipoFront === 1 ? 'Teléfono' : 'Referencia' }}</label>
              <input id="referencia" type="text" class="input num" formControlName="telefonoReferencia" inputmode="numeric"
                     [class.invalid]="compraForm.controls.telefonoReferencia.touched && compraForm.controls.telefonoReferencia.invalid" />
              @if (compraForm.controls.telefonoReferencia.touched && compraForm.controls.telefonoReferencia.invalid) {
                <span class="field-error">Mínimo 8 caracteres</span>
              }
            </div>

            <div class="grid-2">
              <div class="field">
                <label for="monto">Importe (MXN)</label>
                <input id="monto" type="number" step="0.01" class="input num" formControlName="monto" [readonly]="esPrecioFijo()" />
              </div>
              <div class="field">
                <label for="pin">PIN o contraseña</label>
                <input id="pin" type="password" class="input" formControlName="pin" autocomplete="off" />
              </div>
            </div>

            <button type="submit" class="btn btn-primary btn-block"
                    [disabled]="procesando() || compraForm.invalid || estadoResultado() === 'EXITOSA'">
              @if (procesando()) { <span class="spinner"></span> } @else { Continuar }
            </button>
          </form>

          <aside class="stack">
            @switch (estadoResultado()) {
              @case ('EXITOSA') {
                @if (respuestaTx(); as tx) {
                  <div class="surface pad">
                    <span class="badge badge-success">Pago realizado</span>
                    <div class="total num">{{ tx.montoDebitado | currency: 'MXN' : 'symbol-narrow' : '1.2-2' }}</div>
                    <dl class="dl">
                      <div><dt>Transacción</dt><dd class="num">#{{ tx.idTransaccion }}</dd></div>
                      <div><dt>Autorización</dt><dd class="num">{{ tx.numeroAutorizacion || '—' }}</dd></div>
                      <div><dt>Folio</dt><dd class="num">{{ tx.idTxGestoPago || '—' }}</dd></div>
                      <div><dt>Referencia</dt><dd class="num">{{ tx.telefonoReferencia }}</dd></div>
                    </dl>
                    <div class="modal-actions">
                      <a routerLink="/historial" class="btn btn-secondary">Historial</a>
                      <button type="button" class="btn btn-primary" (click)="reiniciarParaNuevaCompra()">Nuevo pago</button>
                    </div>
                  </div>
                }
              }
              @case ('PENDIENTE') {
                <div class="surface pad">
                  <div class="alert alert-warning"><strong>En proceso.</strong> {{ mensajeEstado() }}</div>
                  @if (respuestaTx()?.idTransaccion) {
                    <button type="button" class="btn btn-primary btn-block" [disabled]="consultandoEstado()" (click)="verificarEstadoTransaccion()">
                      @if (consultandoEstado()) { <span class="spinner"></span> } @else { Verificar estado }
                    </button>
                  }
                </div>
              }
              @case ('RECHAZADA') {
                <div class="surface pad">
                  <div class="alert alert-danger"><strong>Pago rechazado.</strong> {{ mensajeEstado() }}</div>
                  <button type="button" class="btn btn-secondary btn-block" (click)="regenerarIdempotencia()">Intentar de nuevo</button>
                </div>
              }
              @case ('ERROR_RED') {
                <div class="surface pad">
                  <div class="alert alert-danger"><strong>Sin respuesta.</strong> {{ mensajeEstado() }} Revisa el historial antes de reintentar.</div>
                  <div class="modal-actions" style="margin-top:0">
                    <a routerLink="/historial" class="btn btn-secondary">Historial</a>
                    <button type="button" class="btn btn-primary" (click)="abrirConfirmacion()">Reintentar</button>
                  </div>
                </div>
              }
              @default {
                <div class="surface pad">
                  <h3>Resumen</h3>
                  <dl class="dl">
                    <div><dt>Importe</dt><dd class="num">{{ montoActual() | currency: 'MXN' : 'symbol-narrow' : '1.2-2' }}</dd></div>
                    @if (comercioService.cuentaActiva(); as cta) {
                      <div><dt>Saldo actual</dt><dd class="num">{{ cta.saldo | currency: 'MXN' : 'symbol-narrow' : '1.2-2' }}</dd></div>
                      <div>
                        <dt>Saldo después</dt>
                        <dd class="num" [class.neg]="saldoProyectado() < 0">{{ saldoProyectado() | currency: 'MXN' : 'symbol-narrow' : '1.2-2' }}</dd>
                      </div>
                    }
                  </dl>
                </div>
              }
            }
          </aside>
        </div>
      }
    </div>

    @if (mostrarConfirmacion()) {
      <div class="modal-backdrop" role="dialog" aria-modal="true">
        <div class="modal">
          <h2>Confirmar pago</h2>
          <dl class="dl">
            <div><dt>Servicio</dt><dd>{{ producto()?.producto }}</dd></div>
            <div><dt>Referencia</dt><dd class="num">{{ compraForm.controls.telefonoReferencia.value }}</dd></div>
            <div><dt>Cuenta</dt><dd class="num">•••• {{ compraForm.controls.numeroCuenta.value.slice(-4) }}</dd></div>
            <div><dt>Importe</dt><dd class="num">{{ compraForm.controls.monto.value | currency: 'MXN' : 'symbol-narrow' : '1.2-2' }}</dd></div>
          </dl>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" [disabled]="procesando()" (click)="mostrarConfirmacion.set(false)">Cancelar</button>
            <button type="button" class="btn btn-primary" [disabled]="procesando()" (click)="ejecutarCompra()">Pagar</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .split { display: grid; grid-template-columns: 1.4fr 1fr; gap: 1.25rem; align-items: start; }
    .svc { display: flex; align-items: center; gap: 1rem; padding-bottom: 1rem; margin-bottom: 1rem; border-bottom: 1px solid var(--line); }
    .grow { flex: 1; min-width: 0; }
    .total { font-size: 1.75rem; font-weight: 700; margin: 0.6rem 0 0.4rem; }
    .neg { color: var(--danger); }
    @media (max-width: 860px) { .split { grid-template-columns: 1fr; } }
  `]
})
export class CompraComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  readonly comercioService = inject(ComercioService);

  readonly producto = computed<ProductoItem | null>(() => this.comercioService.productoSeleccionado());
  readonly idempotencyKey = signal(this.comercioService.generarIdempotencyKey());
  readonly procesando = signal(false);
  readonly consultandoEstado = signal(false);
  readonly mostrarConfirmacion = signal(false);
  readonly estadoResultado = signal<EstadoResultado>('NONE');
  readonly mensajeEstado = signal('');
  readonly respuestaTx = signal<PagoServicioResponse | null>(null);
  readonly montoActual = signal(0);

  readonly compraForm = this.fb.nonNullable.group({
    numeroCuenta: ['', [Validators.required, Validators.minLength(10)]],
    telefonoReferencia: ['', [Validators.required, Validators.minLength(8)]],
    monto: [0, [Validators.required, Validators.min(1)]],
    pin: ['', [Validators.required, Validators.minLength(4)]]
  });

  readonly saldoProyectado = computed(() => (this.comercioService.cuentaActiva()?.saldo ?? 0) - this.montoActual());

  constructor() {
    effect(() => {
      const activa = this.comercioService.cuentaActiva();
      if (activa && this.compraForm.controls.numeroCuenta.value !== activa.numeroCuenta) {
        this.compraForm.controls.numeroCuenta.setValue(activa.numeroCuenta);
      }
    });
    this.compraForm.controls.monto.valueChanges.subscribe((v) => this.montoActual.set(Number(v) || 0));
  }

  ngOnInit(): void {
    const prod = this.producto();
    if (prod && this.esPrecioFijo()) {
      this.compraForm.controls.monto.setValue(Number.parseFloat(prod.precio));
    }
    const activa = this.comercioService.cuentaActiva();
    if (activa) {
      this.compraForm.controls.numeroCuenta.setValue(activa.numeroCuenta);
    } else {
      const email = this.authService.userEmail();
      if (email) {
        this.comercioService.cargarPerfilYCuentas(email);
      }
    }
  }

  alCambiarCuenta(): void {
    const val = this.compraForm.controls.numeroCuenta.value;
    if (val) {
      this.comercioService.seleccionarCuentaActiva(val);
    }
  }

  esPrecioFijo(): boolean {
    const val = Number.parseFloat(this.producto()?.precio ?? '');
    return !Number.isNaN(val) && val > 0;
  }

  abrirConfirmacion(): void {
    if (this.compraForm.invalid || this.procesando()) {
      this.compraForm.markAllAsTouched();
      return;
    }
    this.mostrarConfirmacion.set(true);
  }

  ejecutarCompra(): void {
    const prod = this.producto();
    if (!prod || this.procesando()) {
      return;
    }
    this.mostrarConfirmacion.set(false);
    this.procesando.set(true);
    this.estadoResultado.set('NONE');
    this.mensajeEstado.set('');

    const formVal = this.compraForm.getRawValue();
    this.comercioService
      .ejecutarPagoServicio(
        {
          numeroCuenta: formVal.numeroCuenta.trim(),
          idProducto: prod.idProducto,
          idServicio: prod.idServicio,
          telefonoReferencia: formVal.telefonoReferencia.trim(),
          monto: formVal.monto,
          pin: formVal.pin
        },
        this.idempotencyKey()
      )
      .pipe(timeout(25000))
      .subscribe({
        next: (resp) => {
          this.procesando.set(false);
          const tx = resp.datos ?? null;
          this.respuestaTx.set(tx);
          if (tx?.estatus === 'EN_PROCESO') {
            this.estadoResultado.set('PENDIENTE');
            this.mensajeEstado.set(tx.mensaje || resp.mensaje);
          } else {
            this.estadoResultado.set('EXITOSA');
            this.mensajeEstado.set(resp.mensaje);
            this.comercioService.actualizarSaldoCuentaActiva(tx?.saldoRestante);
          }
        },
        error: (err: unknown) => {
          this.procesando.set(false);
          if (err instanceof TimeoutError) {
            this.estadoResultado.set('ERROR_RED');
            this.mensajeEstado.set('Tiempo de espera agotado.');
            return;
          }
          const httpErr = err as HttpErrorResponse;
          const body = httpErr.error as { mensaje?: string } | null;
          if (httpErr.status === 0 || httpErr.status === 504 || httpErr.status === 503) {
            this.estadoResultado.set('ERROR_RED');
            this.mensajeEstado.set(body?.mensaje || 'No hubo respuesta del servidor.');
          } else {
            this.estadoResultado.set('RECHAZADA');
            this.mensajeEstado.set(body?.mensaje || 'La transacción fue rechazada.');
          }
        }
      });
  }

  verificarEstadoTransaccion(): void {
    const idTx = this.respuestaTx()?.idTransaccion;
    if (!idTx || this.consultandoEstado()) {
      return;
    }
    this.consultandoEstado.set(true);
    this.comercioService.consultarEstadoTransaccion(idTx).subscribe({
      next: (resp) => {
        this.consultandoEstado.set(false);
        const tx = resp.datos;
        if (!tx) {
          return;
        }
        this.respuestaTx.set(tx);
        if (tx.estatus === 'EXITOSA') {
          this.estadoResultado.set('EXITOSA');
          this.comercioService.actualizarSaldoCuentaActiva(tx.saldoRestante);
        } else if (tx.estatus === 'FALLIDA' || tx.estatus === 'REVERSADA') {
          this.estadoResultado.set('RECHAZADA');
          this.mensajeEstado.set(tx.mensaje);
        } else {
          this.estadoResultado.set('PENDIENTE');
          this.mensajeEstado.set(tx.mensaje);
        }
      },
      error: () => this.consultandoEstado.set(false)
    });
  }

  regenerarIdempotencia(): void {
    this.idempotencyKey.set(this.comercioService.generarIdempotencyKey());
    this.estadoResultado.set('NONE');
    this.mensajeEstado.set('');
  }

  reiniciarParaNuevaCompra(): void {
    this.comercioService.productoSeleccionado.set(null);
    this.idempotencyKey.set(this.comercioService.generarIdempotencyKey());
    void this.router.navigate(['/catalogo']);
  }
}
