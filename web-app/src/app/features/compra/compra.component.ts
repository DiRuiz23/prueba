import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TimeoutError, timeout } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ComercioService } from '../../core/services/comercio.service';
import { GenericResponse, PagoServicioResponse, ProductoItem } from '../../core/models/api.models';
import { environment } from '../../../environments/environment';

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
              @if (tieneImagenServicio()) {
                <span class="svc-avatar">
                  <img class="svc-avatar-img" [src]="urlImagenServicio()" [alt]="producto()!.servicio"
                       (error)="imagenFallida.set(true)" />
                </span>
              }
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
                <input id="numeroCuenta" type="text" inputmode="numeric" maxlength="13" class="input num"
                       formControlName="numeroCuenta" (input)="sanitizarCuenta()" />
              </div>
            }

            <div class="field">
              <label for="referencia">{{ producto()!.tipoFront === 1 ? 'Teléfono' : 'Referencia' }}</label>
              <input id="referencia" type="text" class="input num" formControlName="telefonoReferencia"
                     [attr.inputmode]="producto()!.tipoFront === 1 ? 'numeric' : 'text'"
                     [attr.maxlength]="producto()!.tipoFront === 1 ? 10 : 30"
                     (input)="sanitizarReferencia()"
                     [class.invalid]="compraForm.controls.telefonoReferencia.touched && compraForm.controls.telefonoReferencia.invalid" />
              @if (compraForm.controls.telefonoReferencia.touched && compraForm.controls.telefonoReferencia.invalid) {
                <span class="field-error">{{ producto()!.tipoFront === 1 ? 'Ingresa 10 dígitos numéricos' : 'Entre 4 y 30 caracteres alfanuméricos' }}</span>
              }
            </div>

            <div class="grid-2">
              <div class="field">
                <label for="monto">Importe (MXN)</label>
                <input id="monto" type="text" inputmode="decimal" maxlength="10" class="input num"
                       formControlName="monto" [readonly]="esPrecioFijo()" (input)="sanitizarMonto()" />
              </div>
              <div class="field">
                <label for="pin">PIN o contraseña</label>
                <input id="pin" type="password" maxlength="64" class="input" formControlName="pin" autocomplete="off" />
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
                  <div class="modal-actions" style="margin-top:0">
                    @if (ticketRechazo()) {
                      <button type="button" class="btn btn-secondary" (click)="mostrarModalRechazo.set(true)">Ver ticket</button>
                    }
                    <button type="button" class="btn btn-primary" (click)="regenerarIdempotencia()">Intentar de nuevo</button>
                  </div>
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
            <div><dt>Importe</dt><dd class="num">{{ montoActual() | currency: 'MXN' : 'symbol-narrow' : '1.2-2' }}</dd></div>
          </dl>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" [disabled]="procesando()" (click)="mostrarConfirmacion.set(false)">Cancelar</button>
            <button type="button" class="btn btn-primary" [disabled]="procesando()" (click)="ejecutarCompra()">Pagar</button>
          </div>
        </div>
      </div>
    }

    @if (mostrarModalRechazo() && ticketRechazo(); as ticket) {
      <div class="modal-backdrop" role="alertdialog" aria-modal="true">
        <div class="modal reject-modal">
          <div class="row-between" style="margin-bottom: 0.75rem;">
            <span class="badge-reject">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              Pago rechazado
            </span>
            <button type="button" class="icon-btn" aria-label="Cerrar" (click)="mostrarModalRechazo.set(false)">✕</button>
          </div>

          <div class="total num">{{ ticket.montoDebitado | currency: 'MXN' : 'symbol-narrow' : '1.2-2' }}</div>

          <div class="alert alert-danger" style="margin-bottom: 0.9rem;">
            <strong>Motivo del rechazo:</strong><br />
            {{ ticket.mensaje }}
          </div>

          <div class="ticket-label">TICKET DE TRANSACCIÓN</div>
          <div class="ticket-box">
            <dl class="dl">
              @if (producto(); as prod) {
                <div><dt>Servicio</dt><dd>{{ prod.producto }}</dd></div>
              }
              <div><dt>Estatus</dt><dd style="color: var(--danger);">{{ ticket.estatus }}</dd></div>
              @if (ticket.idTransaccion > 0) {
                <div><dt>Transacción</dt><dd class="num">#{{ ticket.idTransaccion }}</dd></div>
              }
              <div><dt>UPC</dt><dd class="num">{{ ticket.upc || 'No generado' }}</dd></div>
              @if (ticket.idTxGestoPago) {
                <div><dt>Folio proveedor</dt><dd class="num">{{ ticket.idTxGestoPago }}</dd></div>
              }
              <div><dt>Cuenta</dt><dd class="num">•••• {{ ticket.numeroCuenta.slice(-4) }}</dd></div>
              <div><dt>Referencia</dt><dd class="num">{{ ticket.telefonoReferencia }}</dd></div>
              <div><dt>Fecha</dt><dd class="num">{{ ticket.fechaTransaccion | date: 'dd/MM/yyyy HH:mm' }}</dd></div>
            </dl>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" (click)="descargarTicketRechazo(ticket)">Descargar ticket</button>
            <button type="button" class="btn btn-secondary" (click)="mostrarModalRechazo.set(false)">Cerrar</button>
            <button type="button" class="btn btn-primary" (click)="cerrarYReintentar()">Intentar de nuevo</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .split { display: grid; grid-template-columns: 1.4fr 1fr; gap: 1.25rem; align-items: start; }
    .svc { display: flex; align-items: center; gap: 1rem; padding-bottom: 1rem; margin-bottom: 1rem; border-bottom: 1px solid var(--line); }
    .svc-avatar { width: 46px; height: 46px; border-radius: 12px; border: 1px solid var(--line); background: #fff; padding: 4px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; overflow: hidden; }
    .svc-avatar-img { width: 100%; height: 100%; object-fit: contain; display: block; }
    .grow { flex: 1; min-width: 0; }
    .total { font-size: 1.75rem; font-weight: 700; margin: 0.6rem 0 0.4rem; }
    .neg { color: var(--danger); }
    .reject-modal { max-width: 460px; max-height: 88vh; overflow-y: auto; border: 1px solid rgba(192, 57, 43, 0.35); }
    .reject-modal .alert { word-break: break-word; max-height: 180px; overflow-y: auto; }
    .badge-reject { display: inline-flex; align-items: center; gap: 0.4rem; padding: 0.3rem 0.75rem; border-radius: 999px; background: var(--danger-soft); color: var(--danger); font-weight: 700; font-size: 0.82rem; }
    .ticket-label { font-size: 0.72rem; font-weight: 700; letter-spacing: 0.08em; color: var(--ink-3); margin-bottom: 0.4rem; }
    .ticket-box { background: var(--bg); border: 1px dashed var(--line-strong); border-radius: var(--radius); padding: 0.35rem 0.9rem; }
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
  readonly mostrarModalRechazo = signal(false);
  readonly ticketRechazo = signal<PagoServicioResponse | null>(null);
  readonly estadoResultado = signal<EstadoResultado>('NONE');
  readonly mensajeEstado = signal('');
  readonly respuestaTx = signal<PagoServicioResponse | null>(null);
  readonly montoActual = signal(0);
  readonly imagenFallida = signal(false);

  readonly compraForm = this.fb.nonNullable.group({
    numeroCuenta: ['', [Validators.required, Validators.pattern(/^\d{10,13}$/)]],
    telefonoReferencia: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9\-_]{4,30}$/)]],
    monto: ['', [Validators.required, Validators.pattern(/^\d{1,7}(\.\d{1,2})?$/), Validators.min(1)]],
    pin: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(64)]]
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
    this.imagenFallida.set(false);
    if (prod) {
      if (prod.tipoFront === 1) {
        this.compraForm.controls.telefonoReferencia.setValidators([
          Validators.required,
          Validators.pattern(/^\d{10}$/)
        ]);
      } else {
        this.compraForm.controls.telefonoReferencia.setValidators([
          Validators.required,
          Validators.pattern(/^[A-Za-z0-9\-_]{4,30}$/)
        ]);
      }
      this.compraForm.controls.telefonoReferencia.updateValueAndValidity();
      if (this.esPrecioFijo()) {
        this.compraForm.controls.monto.setValue(String(Number.parseFloat(prod.precio)));
      }
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

  tieneImagenServicio(): boolean {
    const id = this.producto()?.idServicio ?? 0;
    return id > 0 && !this.imagenFallida();
  }

  urlImagenServicio(): string {
    return `${environment.apiUrl}/gestopago/servicios/${this.producto()?.idServicio ?? 0}/imagen`;
  }

  sanitizarCuenta(): void {
    const ctrl = this.compraForm.controls.numeroCuenta;
    const limpio = ctrl.value.replace(/\D/g, '').slice(0, 13);
    if (ctrl.value !== limpio) ctrl.setValue(limpio);
  }

  sanitizarReferencia(): void {
    const ctrl = this.compraForm.controls.telefonoReferencia;
    const esRecarga = this.producto()?.tipoFront === 1;
    const limpio = esRecarga
      ? ctrl.value.replace(/\D/g, '').slice(0, 10)
      : ctrl.value.replace(/[^A-Za-z0-9\-_]/g, '').slice(0, 30);
    if (ctrl.value !== limpio) ctrl.setValue(limpio);
  }

  sanitizarMonto(): void {
    const ctrl = this.compraForm.controls.monto;
    let val = String(ctrl.value ?? '').replace(/[^0-9.]/g, '');
    const partes = val.split('.');
    if (partes.length > 2) {
      val = `${partes[0]}.${partes.slice(1).join('')}`;
    }
    const [ent, dec] = val.split('.');
    val = dec !== undefined ? `${ent.slice(0, 7)}.${dec.slice(0, 2)}` : ent.slice(0, 7);
    if (ctrl.value !== val) ctrl.setValue(val);
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
          monto: Number(formVal.monto),
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
          const body = httpErr.error as GenericResponse<PagoServicioResponse> | null;
          if (httpErr.status === 0 || httpErr.status === 504 || httpErr.status === 503) {
            this.estadoResultado.set('ERROR_RED');
            this.mensajeEstado.set(body?.mensaje || 'No hubo respuesta del servidor.');
          } else {
            const msg = body?.mensaje || 'La transacción fue rechazada.';
            const ticket: PagoServicioResponse = body?.datos ?? {
              idTransaccion: 0,
              upc: 'No generado (validación previa)',
              numeroCuenta: formVal.numeroCuenta.trim(),
              montoDebitado: Number(formVal.monto) || 0,
              telefonoReferencia: formVal.telefonoReferencia.trim(),
              idProducto: prod.idProducto,
              idServicio: prod.idServicio,
              estatus: 'RECHAZADA',
              mensaje: msg,
              fechaTransaccion: new Date().toISOString()
            };
            this.ticketRechazo.set(ticket);
            if (!(ticket.idTransaccion > 0)) {
              this.comercioService.registrarTicketHistorialLocal(ticket);
            }
            this.estadoResultado.set('RECHAZADA');
            this.mensajeEstado.set(msg);
            this.mostrarModalRechazo.set(true);
            if (ticket.saldoRestante !== undefined) {
              this.comercioService.actualizarSaldoCuentaActiva(ticket.saldoRestante);
            }
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
          this.ticketRechazo.set(tx);
          this.estadoResultado.set('RECHAZADA');
          this.mensajeEstado.set(tx.mensaje);
          this.mostrarModalRechazo.set(true);
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
    this.ticketRechazo.set(null);
    this.mostrarModalRechazo.set(false);
  }

  cerrarYReintentar(): void {
    this.regenerarIdempotencia();
  }

  descargarTicketRechazo(ticket: PagoServicioResponse): void {
    this.comercioService.descargarTicketTxt(ticket, this.producto()?.producto);
  }

  reiniciarParaNuevaCompra(): void {
    this.comercioService.productoSeleccionado.set(null);
    this.idempotencyKey.set(this.comercioService.generarIdempotencyKey());
    void this.router.navigate(['/catalogo']);
  }
}
