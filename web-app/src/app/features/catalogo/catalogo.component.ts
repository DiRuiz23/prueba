import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ComercioService } from '../../core/services/comercio.service';
import { ProductoItem } from '../../core/models/api.models';

interface MarcaVisual {
  siglas: string;
  bg: string;
  color: string;
}

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <div class="page-head">
        <h1>Servicios</h1>
        <button type="button" class="icon-btn" aria-label="Actualizar catálogo" [disabled]="cargando()" (click)="cargarProductos(true)">
          <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-3-6.7L21 8"/><path d="M21 3v5h-5"/></svg>
        </button>
      </div>

      <div class="toolbar">
        <div class="search">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
          <input type="search" class="input" placeholder="Buscar" aria-label="Buscar servicio"
                 [ngModel]="busqueda()" (ngModelChange)="onBuscar($event)" />
        </div>
        <div class="seg" role="tablist">
          <button type="button" [class.on]="filtroTipo() === 'TODOS'" (click)="onCambiarFiltro('TODOS')">Todos</button>
          <button type="button" [class.on]="filtroTipo() === 'RECARGA'" (click)="onCambiarFiltro('RECARGA')">Recargas</button>
          <button type="button" [class.on]="filtroTipo() === 'SERVICIO'" (click)="onCambiarFiltro('SERVICIO')">Servicios</button>
        </div>
      </div>

      @if (cargando()) {
        <div class="center"><div class="spinner dark"></div></div>
      } @else if (errorMsg()) {
        <div class="alert alert-danger">
          {{ errorMsg() }}
          <div style="margin-top: 0.6rem;"><button type="button" class="btn btn-secondary" (click)="cargarProductos(true)">Reintentar</button></div>
        </div>
      } @else if (productosPaginados().length === 0) {
        <div class="surface center"><span class="muted">Sin resultados</span></div>
      } @else {
        <div class="list">
          @for (item of productosPaginados(); track item.idProducto + '-' + item.idServicio) {
            <button type="button" class="item" (click)="seleccionarProducto(item)">
              <span class="avatar" [style.background]="estiloMarca(item).bg" [style.color]="estiloMarca(item).color">{{ estiloMarca(item).siglas }}</span>
              <span class="grow">
                <span class="title" style="display:block">{{ item.producto }}</span>
                <span class="sub" style="display:block">{{ item.servicio }}</span>
              </span>
              <span class="end">
                <span class="amount">{{ esPrecioFijo(item.precio) ? (item.precio | currency: 'MXN' : 'symbol-narrow' : '1.2-2') : 'Variable' }}</span>
              </span>
            </button>
          }
        </div>

        @if (totalPaginas() > 1) {
          <div class="pager">
            <button type="button" class="btn btn-secondary" [disabled]="paginaActual() === 0" (click)="paginaActual.set(paginaActual() - 1)">Anterior</button>
            <span class="num">{{ paginaActual() + 1 }} / {{ totalPaginas() }}</span>
            <button type="button" class="btn btn-secondary" [disabled]="paginaActual() >= totalPaginas() - 1" (click)="paginaActual.set(paginaActual() + 1)">Siguiente</button>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .toolbar { display: flex; flex-wrap: wrap; gap: 0.75rem; margin-bottom: 1rem; }
  `]
})
export class CatalogoComponent implements OnInit {
  private readonly comercioService = inject(ComercioService);
  private readonly router = inject(Router);

  readonly productos = signal<ProductoItem[]>([]);
  readonly cargando = signal(false);
  readonly errorMsg = signal<string | null>(null);
  readonly busqueda = signal('');
  readonly filtroTipo = signal<'TODOS' | 'RECARGA' | 'SERVICIO'>('TODOS');
  readonly paginaActual = signal(0);
  readonly tamanioPagina = 12;

  readonly productosFiltrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    const tipo = this.filtroTipo();
    return this.productos().filter((p) => {
      const coincideTipo =
        tipo === 'TODOS' ||
        (tipo === 'RECARGA' && p.tipoFront === 1) ||
        (tipo === 'SERVICIO' && p.tipoFront !== 1);
      const coincideTexto =
        !q ||
        (p.producto && p.producto.toLowerCase().includes(q)) ||
        (p.servicio && p.servicio.toLowerCase().includes(q));
      return coincideTipo && coincideTexto;
    });
  });

  readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.productosFiltrados().length / this.tamanioPagina))
  );

  readonly productosPaginados = computed(() => {
    const inicio = this.paginaActual() * this.tamanioPagina;
    return this.productosFiltrados().slice(inicio, inicio + this.tamanioPagina);
  });

  ngOnInit(): void {
    this.cargarProductos(false);
  }

  cargarProductos(forzar: boolean): void {
    this.cargando.set(true);
    this.errorMsg.set(null);
    this.comercioService.obtenerProductos(forzar).subscribe({
      next: (lista) => {
        this.productos.set(lista);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        const body = err.error as { mensaje?: string } | null;
        this.errorMsg.set(body?.mensaje || 'No fue posible consultar el catálogo.');
      }
    });
  }

  onBuscar(valor: string): void {
    this.busqueda.set(valor);
    this.paginaActual.set(0);
  }

  onCambiarFiltro(tipo: 'TODOS' | 'RECARGA' | 'SERVICIO'): void {
    this.filtroTipo.set(tipo);
    this.paginaActual.set(0);
  }

  esPrecioFijo(precio?: string): boolean {
    const num = Number.parseFloat(precio ?? '');
    return !Number.isNaN(num) && num > 0;
  }

  estiloMarca(item: ProductoItem): MarcaVisual {
    const t = `${item.servicio} ${item.producto}`.toLowerCase();
    if (t.includes('telcel')) return { siglas: 'TC', bg: '#e8efff', color: '#1d4ed8' };
    if (t.includes('at&t') || t.includes('att')) return { siglas: 'AT', bg: '#e0f2fe', color: '#0369a1' };
    if (t.includes('movistar')) return { siglas: 'MV', bg: '#e6f6ec', color: '#15803d' };
    if (t.includes('cfe')) return { siglas: 'CFE', bg: '#e8f5ef', color: '#0b7a5a' };
    if (t.includes('totalplay')) return { siglas: 'TP', bg: '#f1e9fb', color: '#6b21a8' };
    if (t.includes('telmex') || t.includes('izzi')) return { siglas: 'TX', bg: '#fdecea', color: '#b42318' };
    if (t.includes('netflix')) return { siglas: 'NF', bg: '#fdecea', color: '#b91c1c' };
    if (t.includes('spotify')) return { siglas: 'SP', bg: '#e6f6ec', color: '#166534' };
    const base = (item.servicio || item.producto || 'GP').trim();
    return { siglas: base.substring(0, 2).toUpperCase(), bg: '#eef0f4', color: '#475569' };
  }

  seleccionarProducto(producto: ProductoItem): void {
    this.comercioService.productoSeleccionado.set(producto);
    void this.router.navigate(['/compra']);
  }
}
