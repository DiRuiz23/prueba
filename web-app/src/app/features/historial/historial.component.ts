import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ComercioService } from '../../core/services/comercio.service';
import { PagoServicioResponse } from '../../core/models/api.models';

@Component({
  selector: 'app-historial',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page">
      <div class="page-head">
        <h1>Historial</h1>
        <button type="button" class="icon-btn" aria-label="Actualizar" [disabled]="cargando()" (click)="cargarHistorial(paginaActual())">
          <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-3-6.7L21 8"/><path d="M21 3v5h-5"/></svg>
        </button>
      </div>

      @if (cargando()) {
        <div class="center"><div class="spinner dark"></div></div>
      } @else if (errorMsg()) {
        <div class="alert alert-danger">
          {{ errorMsg() }}
          <div style="margin-top: 0.6rem;"><button type="button" class="btn btn-secondary" (click)="cargarHistorial(paginaActual())">Reintentar</button></div>
        </div>
      } @else if (operaciones().length === 0) {
        <div class="surface center"><span class="muted">Sin operaciones</span></div>
      } @else {
        <div class="list">
          @for (item of operaciones(); track item.idTransaccion) {
            <button type="button" class="item" (click)="detalleSeleccionado.set(item)">
              <span class="grow">
                <span class="title" style="display:block">Producto #{{ item.idProducto || '—' }} · <span class="num">{{ item.telefonoReferencia }}</span></span>
                <span class="sub num" style="display:block">{{ item.fechaTransaccion | date: 'dd MMM yyyy, HH:mm' }}</span>
              </span>
              <span class="end">
                <span class="amount" style="display:block">{{ item.montoDebitado | currency: 'MXN' : 'symbol-narrow' : '1.2-2' }}</span>
                <span class="badge badge-success">Exitosa</span>
              </span>
            </button>
          }
        </div>

        <div class="pager">
          <button type="button" class="btn btn-secondary" [disabled]="paginaActual() === 0" (click)="cargarHistorial(paginaActual() - 1)">Anterior</button>
          <span class="num">{{ paginaActual() + 1 }} / {{ totalPaginas() }}</span>
          <button type="button" class="btn btn-secondary" [disabled]="esUltimaPagina()" (click)="cargarHistorial(paginaActual() + 1)">Siguiente</button>
        </div>
      }
    </div>

    @if (detalleSeleccionado(); as det) {
      <div class="modal-backdrop" role="dialog" aria-modal="true" (click)="detalleSeleccionado.set(null)">
        <div class="modal" (click)="$event.stopPropagation()">
          <span class="badge badge-success">Exitosa</span>
          <div class="total num">{{ det.montoDebitado | currency: 'MXN' : 'symbol-narrow' : '1.2-2' }}</div>
          <dl class="dl">
            <div><dt>Fecha</dt><dd class="num">{{ det.fechaTransaccion | date: 'dd/MM/yyyy HH:mm:ss' }}</dd></div>
            <div><dt>Producto</dt><dd class="num">#{{ det.idProducto }} / #{{ det.idServicio }}</dd></div>
            <div><dt>Cuenta</dt><dd class="num">•••• {{ det.numeroCuenta.slice(-4) }}</dd></div>
            <div><dt>Referencia</dt><dd class="num">{{ det.telefonoReferencia }}</dd></div>
            <div><dt>Autorización</dt><dd class="num">{{ det.numeroAutorizacion || '—' }}</dd></div>
            <div><dt>Folio</dt><dd class="num">{{ det.idTxGestoPago || '—' }}</dd></div>
            <div><dt>UPC</dt><dd class="num">{{ det.upc }}</dd></div>
          </dl>
          <div class="modal-actions">
            <button type="button" class="btn btn-primary" (click)="detalleSeleccionado.set(null)">Cerrar</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .total { font-size: 1.75rem; font-weight: 700; margin: 0.6rem 0 0.4rem; }
  `]
})
export class HistorialComponent implements OnInit {
  private readonly comercioService = inject(ComercioService);

  readonly operaciones = signal<PagoServicioResponse[]>([]);
  readonly cargando = signal(false);
  readonly errorMsg = signal<string | null>(null);
  readonly paginaActual = signal(0);
  readonly totalPaginas = signal(1);
  readonly esUltimaPagina = signal(true);
  readonly detalleSeleccionado = signal<PagoServicioResponse | null>(null);

  ngOnInit(): void {
    this.cargarHistorial(0);
  }

  cargarHistorial(pagina: number): void {
    this.cargando.set(true);
    this.errorMsg.set(null);
    this.comercioService.obtenerHistorialExitoso(pagina, 10).subscribe({
      next: (page) => {
        this.operaciones.set(page.content);
        this.paginaActual.set(page.number);
        this.totalPaginas.set(Math.max(1, page.totalPages));
        this.esUltimaPagina.set(page.last);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        const body = err.error as { mensaje?: string } | null;
        this.errorMsg.set(body?.mensaje || 'No fue posible consultar el historial.');
      }
    });
  }
}
