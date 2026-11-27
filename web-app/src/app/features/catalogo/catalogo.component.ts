import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ComercioService } from '../../core/services/comercio.service';
import { ProductoItem } from '../../core/models/api.models';
import { environment } from '../../../environments/environment';

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
          <input type="search" maxlength="60" class="input" placeholder="Buscar" aria-label="Buscar servicio"
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
        <div class="cards-grid">
          @for (item of productosPaginados(); track item.idProducto + '-' + item.idServicio) {
            <article class="svc-card" (click)="seleccionarProducto(item)">
              <div class="card-top">
                @if (tieneImagen(item.idServicio)) {
                  <span class="avatar-circle has-img">
                    <img class="avatar-img" [src]="urlImagenServicio(item.idServicio)" [alt]="item.servicio"
                         loading="lazy" (error)="marcarImagenFallida(item.idServicio)" />
                  </span>
                } @else {
                  <span class="avatar-circle" [style.background]="estiloMarca(item).bg" [style.color]="estiloMarca(item).color">
                    {{ estiloMarca(item).siglas }}
                  </span>
                }
                <span class="cat-pill" [class.recarga]="item.tipoFront === 1">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
                  {{ item.tipoFront === 1 ? 'Recarga' : 'Servicio' }}
                </span>
              </div>

              <div class="card-body">
                <div class="provider-line">
                  <strong class="provider">{{ item.servicio }}</strong>
                  <span class="faint small">Pago inmediato</span>
                </div>
                <h3 class="card-title">{{ item.producto }}</h3>
                <div class="tags">
                  <span class="tag">{{ item.tipoFront === 1 ? 'Tiempo aire' : 'Pago de servicio' }}</span>
                  <span class="tag">{{ esPrecioFijo(item.precio) ? 'Monto fijo' : 'Monto variable' }}</span>
                </div>
              </div>

              <div class="card-divider"></div>

              <div class="card-foot">
                <div class="price-block">
                  <strong class="price num">
                    {{ esPrecioFijo(item.precio) ? (item.precio | currency: 'MXN' : 'symbol-narrow' : '1.2-2') : 'Variable' }}
                  </strong>
                  <span class="faint small">Cargo a tu cuenta</span>
                </div>
                <button type="button" class="btn-card" (click)="seleccionarProducto(item); $event.stopPropagation()">
                  Pagar ahora
                </button>
              </div>
            </article>
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
    .toolbar { display: flex; flex-wrap: wrap; gap: 0.75rem; margin-bottom: 1.15rem; }
    .cards-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1rem; }
    .svc-card {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 22px;
      padding: 1.25rem;
      display: flex;
      flex-direction: column;
      box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
      cursor: pointer;
      transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease;
    }
    .svc-card:hover {
      transform: translateY(-2px);
      border-color: rgba(11, 122, 90, 0.35);
      box-shadow: 0 10px 24px rgba(11, 122, 90, 0.09);
    }
    .card-top { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; margin-bottom: 0.95rem; }
    .avatar-circle {
      width: 48px; height: 48px; border-radius: 50%;
      border: 1px solid var(--line);
      display: inline-flex; align-items: center; justify-content: center;
      font-weight: 800; font-size: 0.92rem; flex-shrink: 0;
    }
    .avatar-circle.has-img { background: #fff; padding: 5px; overflow: hidden; }
    .avatar-img { width: 100%; height: 100%; object-fit: contain; display: block; }
    .cat-pill {
      display: inline-flex; align-items: center; gap: 0.3rem;
      padding: 0.3rem 0.65rem; border-radius: 10px;
      background: #f1f5f9; color: var(--ink-2); font-size: 0.78rem; font-weight: 600;
    }
    .cat-pill.recarga { background: var(--brand-soft); color: var(--brand); }
    .card-body { flex: 1; }
    .provider-line { display: flex; align-items: baseline; gap: 0.45rem; flex-wrap: wrap; margin-bottom: 0.25rem; }
    .provider { font-size: 0.92rem; font-weight: 700; color: var(--ink); }
    .card-title { font-size: 1.12rem; font-weight: 700; color: var(--ink); margin: 0 0 0.7rem; line-height: 1.3; }
    .tags { display: flex; flex-wrap: wrap; gap: 0.4rem; }
    .tag {
      padding: 0.28rem 0.6rem; border-radius: 8px;
      background: #f1f5f9; color: var(--ink-2); font-size: 0.76rem; font-weight: 600;
    }
    .card-divider { height: 1px; background: var(--line); margin: 1.1rem 0 0.95rem; }
    .card-foot { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; }
    .price-block { display: flex; flex-direction: column; line-height: 1.25; }
    .price { font-size: 1.08rem; font-weight: 800; color: var(--ink); }
    .btn-card {
      border: none; border-radius: 12px; min-height: 40px; padding: 0 1rem;
      background: var(--brand); color: #fff; font: inherit; font-size: 0.86rem; font-weight: 600;
      cursor: pointer; white-space: nowrap; transition: background 0.15s ease;
    }
    .btn-card:hover { background: var(--brand-hover); }
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
  readonly imagenesFallidas = signal<Record<number, boolean>>({});

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

  urlImagenServicio(idServicio: number): string {
    return `${environment.apiUrl}/gestopago/servicios/${idServicio}/imagen`;
  }

  tieneImagen(idServicio?: number): boolean {
    return !!idServicio && idServicio > 0 && !this.imagenesFallidas()[idServicio];
  }

  marcarImagenFallida(idServicio?: number): void {
    if (!idServicio) return;
    this.imagenesFallidas.update((prev) => ({ ...prev, [idServicio]: true }));
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
