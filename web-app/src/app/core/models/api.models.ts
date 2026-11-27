export interface GenericResponse<T> {
  codigo: number;
  mensaje: string;
  datos?: T;
  errores?: Record<string, string>;
}

export interface LoginRequest {
  correo: string;
  password: string;
  dispositivo: 'WEB' | 'MOVIL';
  forzarCierreSesionAnterior?: boolean;
}

export interface VerificarOtpRequest {
  correo: string;
  codigo: string;
  dispositivo: 'WEB' | 'MOVIL';
}

export interface LoginResponse {
  token?: string;
  tipo?: string;
  correo: string;
  requiere2fa: boolean;
  metodo2fa?: 'GOOGLE_AUTHENTICATOR' | string;
  requiereConfiguracionTotp?: boolean;
  totpSecreto?: string;
  totpOtpAuthUri?: string;
  totpQrBase64?: string;
  mensaje?: string;
  sesionActivaDetectada: boolean;
  dispositivoSesionActiva?: string;
  tiempoExpiracionOtpSegundos?: number;
}

export interface ProductoItem {
  producto: string;
  servicio: string;
  idServicio: number;
  idProducto: number;
  idCatTipoServicio: number;
  tipoFront: number;
  hasDigitoVerificador?: boolean;
  precio: string;
  showAyuda?: boolean;
  legend?: string;
}

export interface GestoPagoProductListResponse {
  status: string;
  mensaje: string;
  fechaConsulta: string;
  products: ProductoItem[];
}

export interface ClienteResponse {
  id: number;
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  correo: string;
  numeroCuentas: string[];
}

export interface DomicilioRequest {
  calle: string;
  numeroExterior: string;
  numeroInterior: string | null;
  colonia: string;
  municipio: string;
  estado: string;
  codigoPostal: string;
  pais: string;
}

export interface RegistroClienteRequest {
  nombre: string;
  segundoNombre: string | null;
  apellidoPaterno: string;
  apellidoMaterno: string;
  fechaNacimiento: string;
  curp: string;
  rfc: string;
  sexo: string;
  nacionalidad: string;
  estadoCivil: string;
  correo: string;
  telefonoMovil: string;
  telefonoAlternativo: string | null;
  ocupacion: string;
  empresa: string | null;
  ingresoMensual: number;
  domicilio: DomicilioRequest;
  password: string;
}

export interface UbicacionCatalogoResponse {
  estado: string;
  municipio: string;
  codigoPostal: string;
}

export interface DisponibilidadRegistroResponse {
  curpDisponible: boolean;
  rfcDisponible: boolean;
  correoDisponible: boolean;
  telefonoDisponible?: boolean;
}

export interface TotpEstadoResponse {
  habilitado: boolean;
}

export interface TotpConfigResponse {
  secreto: string;
  otpAuthUri: string;
  qrBase64?: string;
  emisor: string;
  correo: string;
}

export interface CuentaResponse {
  id: number;
  clienteId: number;
  nombreCliente: string;
  numeroCuenta: string;
  saldo: number;
  estatus: 'ACTIVA' | 'BLOQUEADA' | 'INACTIVA';
}

export interface PaginaResponse<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  last: boolean;
  first: boolean;
}

export interface PagoServicioRequest {
  numeroCuenta: string;
  idProducto: number;
  idServicio: number;
  telefonoReferencia: string;
  monto?: number;
  pin: string;
}

export interface PagoServicioResponse {
  idTransaccion: number;
  upc: string;
  idTxGestoPago?: string;
  numeroAutorizacion?: string;
  numeroCuenta: string;
  montoDebitado: number;
  saldoRestante?: number;
  telefonoReferencia: string;
  idProducto?: number;
  idServicio?: number;
  estatus: 'EXITOSA' | 'EN_PROCESO' | 'FALLIDA' | 'REVERSADA' | string;
  mensaje: string;
  fechaTransaccion: string;
}

