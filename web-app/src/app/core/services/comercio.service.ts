import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of, tap, map, switchMap, catchError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ClienteResponse,
  CuentaResponse,
  GenericResponse,
  GestoPagoProductListResponse,
  PagoServicioRequest,
  PagoServicioResponse,
  PaginaResponse,
  ProductoItem,
  SpringPage
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class ComercioService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly ticketsLocalesKey = 'apphasani_tickets_rechazo_locales';

  // Caché en memoria para evitar peticiones repetitivas de catálogo en la sesión
  private catalogoCache: ProductoItem[] | null = null;
  readonly productoSeleccionado = signal<ProductoItem | null>(null);

  // Estado bancario global compartido entre Tarjeta Virtual, Header y Pantallas
  readonly clienteActual = signal<ClienteResponse | null>(null);
  readonly cuentasUsuario = signal<CuentaResponse[]>([]);
  readonly cuentaActiva = signal<CuentaResponse | null>(null);
  readonly cargandoPerfil = signal<boolean>(false);

  cargarPerfilYCuentas(correo: string): void {
    if (!correo || this.cargandoPerfil()) {
      return;
    }
    this.cargandoPerfil.set(true);

    this.http
      .get<GenericResponse<CuentaResponse[]>>(`${this.apiUrl}/cuentas/mias`)
      .pipe(
        map((resp) => resp.datos ?? []),
        catchError(() =>
          this.http
            .post<GenericResponse<PaginaResponse<ClienteResponse>>>(`${this.apiUrl}/clientes/consultar`, {
              correo,
              paginacion: { pagina: 0, tamanio: 1 }
            })
            .pipe(
              switchMap((resp) => {
                const cliente = resp.datos?.contenido?.[0] ?? null;
                this.clienteActual.set(cliente);
                if (!cliente) {
                  return of([] as CuentaResponse[]);
                }
                return this.http
                  .post<GenericResponse<PaginaResponse<CuentaResponse>>>(`${this.apiUrl}/cuentas/consultar`, {
                    clienteId: cliente.id,
                    paginacion: { pagina: 0, tamanio: 10 }
                  })
                  .pipe(map((cResp) => cResp.datos?.contenido ?? []));
              }),
              catchError(() => of([] as CuentaResponse[]))
            )
        )
      )
      .subscribe((cuentas) => {
        this.cuentasUsuario.set(cuentas);
        const activaPrevia = this.cuentaActiva();
        if (activaPrevia) {
          const actualizada = cuentas.find((c) => c.numeroCuenta === activaPrevia.numeroCuenta);
          this.cuentaActiva.set(actualizada ?? cuentas[0] ?? null);
        } else {
          this.cuentaActiva.set(cuentas.find((c) => c.estatus === 'ACTIVA') ?? cuentas[0] ?? null);
        }
        this.cargandoPerfil.set(false);
      });
  }

  seleccionarCuentaActiva(numeroCuenta: string): void {
    const encontrada = this.cuentasUsuario().find((c) => c.numeroCuenta === numeroCuenta);
    if (encontrada) {
      this.cuentaActiva.set(encontrada);
    }
  }

  actualizarSaldoCuentaActiva(nuevoSaldo?: number): void {
    if (nuevoSaldo === undefined || nuevoSaldo === null) {
      return;
    }
    const activa = this.cuentaActiva();
    if (!activa) {
      return;
    }
    const actualizada: CuentaResponse = { ...activa, saldo: nuevoSaldo };
    this.cuentaActiva.set(actualizada);
    this.cuentasUsuario.update((lista) =>
      lista.map((c) => (c.numeroCuenta === activa.numeroCuenta ? actualizada : c))
    );
  }

  limpiarEstadoUsuario(): void {
    this.clienteActual.set(null);
    this.cuentasUsuario.set([]);
    this.cuentaActiva.set(null);
    this.productoSeleccionado.set(null);
    sessionStorage.removeItem(this.ticketsLocalesKey);
  }

  registrarTicketHistorialLocal(ticket: PagoServicioResponse): void {
    const actuales = this.obtenerTicketsLocales();
    const filtrados = actuales.filter(
      (t) => !(t.upc === ticket.upc && t.fechaTransaccion === ticket.fechaTransaccion)
    );
    const actualizados = [ticket, ...filtrados].slice(0, 30);
    try {
      sessionStorage.setItem(this.ticketsLocalesKey, JSON.stringify(actualizados));
    } catch {
      // Ignora límite de sessionStorage
    }
  }

  obtenerTicketsLocales(): PagoServicioResponse[] {
    try {
      const raw = sessionStorage.getItem(this.ticketsLocalesKey);
      if (!raw) return [];
      const parsed = JSON.parse(raw) as unknown;
      return Array.isArray(parsed) ? (parsed as PagoServicioResponse[]) : [];
    } catch {
      return [];
    }
  }

  descargarTicketTxt(ticket: PagoServicioResponse, nombreServicio?: string): void {
    const lineas = [
      '========================================',
      '        APPHASANI - TICKET DE PAGO      ',
      '========================================',
      `Estatus     : ${ticket.estatus}`,
      ...(nombreServicio ? [`Servicio    : ${nombreServicio}`] : []),
      ...(ticket.idTransaccion > 0 ? [`Transaccion : #${ticket.idTransaccion}`] : []),
      `UPC         : ${ticket.upc || 'No generado'}`,
      ...(ticket.idTxGestoPago ? [`Folio       : ${ticket.idTxGestoPago}`] : []),
      ...(ticket.numeroAutorizacion ? [`Autoriz.    : ${ticket.numeroAutorizacion}`] : []),
      `Cuenta      : •••• ${ticket.numeroCuenta.slice(-4)}`,
      `Referencia  : ${ticket.telefonoReferencia}`,
      `Importe     : $${Number(ticket.montoDebitado).toFixed(2)} MXN`,
      `Fecha       : ${new Date(ticket.fechaTransaccion).toLocaleString('es-MX')}`,
      ...(ticket.mensaje
        ? ['----------------------------------------', 'Detalle:', ticket.mensaje]
        : []),
      '========================================'
    ];
    const blob = new Blob([lineas.join('\n')], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `ticket-${(ticket.estatus || 'pago').toLowerCase()}-${ticket.upc || 'apphasani'}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  }

  obtenerProductos(forzarRecarga = false): Observable<ProductoItem[]> {
    if (this.catalogoCache && !forzarRecarga) {
      return of(this.catalogoCache);
    }
    return this.http
      .get<GenericResponse<GestoPagoProductListResponse>>(`${this.apiUrl}/gestopago/productos`)
      .pipe(
        map((resp) => resp.datos?.products ?? []),
        tap((lista) => {
          this.catalogoCache = lista;
        })
      );
  }

  limpiarCacheCatalogo(): void {
    this.catalogoCache = null;
  }

  obtenerCuentasCliente(correo: string): Observable<CuentaResponse[]> {
    return this.http
      .post<GenericResponse<PaginaResponse<ClienteResponse>>>(`${this.apiUrl}/clientes/consultar`, {
        correo,
        paginacion: { pagina: 0, tamanio: 1 }
      })
      .pipe(
        switchMap((resp) => {
          const cliente = resp.datos?.contenido?.[0];
          if (!cliente) {
            return of([]);
          }
          this.clienteActual.set(cliente);
          return this.http
            .post<GenericResponse<PaginaResponse<CuentaResponse>>>(`${this.apiUrl}/cuentas/consultar`, {
              clienteId: cliente.id,
              paginacion: { pagina: 0, tamanio: 10 }
            })
            .pipe(
              map((cResp) => cResp.datos?.contenido ?? []),
              tap((cuentas) => {
                this.cuentasUsuario.set(cuentas);
                if (!this.cuentaActiva() && cuentas.length > 0) {
                  this.cuentaActiva.set(cuentas.find((c) => c.estatus === 'ACTIVA') ?? cuentas[0]);
                }
              })
            );
        })
      );
  }

  ejecutarPagoServicio(
    request: PagoServicioRequest,
    idempotencyKey: string
  ): Observable<GenericResponse<PagoServicioResponse>> {
    const headers = new HttpHeaders({
      'Idempotency-Key': idempotencyKey
    });
    return this.http.post<GenericResponse<PagoServicioResponse>>(
      `${this.apiUrl}/transacciones/pago-servicio`,
      request,
      { headers }
    );
  }

  consultarEstadoTransaccion(idTransaccion: number): Observable<GenericResponse<PagoServicioResponse>> {
    return this.http.get<GenericResponse<PagoServicioResponse>>(
      `${this.apiUrl}/transacciones/${idTransaccion}`
    );
  }

  obtenerHistorialExitoso(page = 0, size = 10): Observable<SpringPage<PagoServicioResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<GenericResponse<SpringPage<PagoServicioResponse>>>(`${this.apiUrl}/transacciones/historial`, { params })
      .pipe(
        map(
          (resp) =>
            resp.datos ?? {
              content: [],
              totalElements: 0,
              totalPages: 0,
              number: 0,
              size,
              last: true,
              first: true
            }
        )
      );
  }

  generarIdempotencyKey(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return 'web-' + Date.now() + '-' + Math.random().toString(36).substring(2, 10);
  }
}
