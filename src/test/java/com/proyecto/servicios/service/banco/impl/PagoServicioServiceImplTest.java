package com.proyecto.servicios.service.banco.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.model.CatProductXml.ProductoItem;
import com.proyecto.servicios.client.model.GestoPagoSendTxPayload;
import com.proyecto.servicios.client.model.GestoPagoTxResponseXml;
import com.proyecto.servicios.dto.request.banco.PagoServicioRequest;
import com.proyecto.servicios.dto.response.banco.PagoServicioResponse;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.enums.EstatusTransaccionServicio;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.banco.IdempotenciaConflictoException;
import com.proyecto.servicios.exception.gestopago.ProveedorException;
import com.proyecto.servicios.exception.gestopago.ResultadoInciertoException;
import com.proyecto.servicios.repository.banco.TransaccionServicioRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.service.banco.AutorizacionOperacionService;
import com.proyecto.servicios.service.banco.CuentaDebitoService;
import com.proyecto.servicios.service.banco.NotificacionTransaccionService;
import com.proyecto.servicios.service.banco.NotificacionTransaccionService.NotificacionTransaccionData;
import com.proyecto.servicios.service.banco.strategy.ValidacionMontoResult;
import com.proyecto.servicios.service.gestopago.CatalogoPagoValidator;
import com.proyecto.servicios.service.gestopago.GestoPagoTxGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: PagoServicioServiceImpl")
class PagoServicioServiceImplTest {

        private static final String CORREO = "usuario@test.com";
        private static final String NUMERO_CUENTA = "012111222333";
        private static final String IDEMPOTENCY_KEY = "idem-abc-123";

        @Mock
        private AutorizacionOperacionService autorizacionOperacionService;
        @Mock
        private CatalogoPagoValidator catalogoPagoValidator;
        @Mock
        private CuentaDebitoService cuentaDebitoService;
        @Mock
        private GestoPagoTxGateway gestoPagoTxGateway;
        @Mock
        private TransaccionServicioRepository transaccionRepository;
        @Mock
        private UsuarioRepository usuarioRepository;
        @Mock
        private NotificacionTransaccionService notificacionTransaccionService;

        @Spy
        private ObjectMapper objectMapper = new ObjectMapper();

        @InjectMocks
        private PagoServicioServiceImpl pagoServicioService;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(pagoServicioService, "unidad", "083");
                ReflectionTestUtils.setField(pagoServicioService, "notificacionTransaccionService",
                                notificacionTransaccionService);
        }

        private PagoServicioRequest crearRequest() {
                PagoServicioRequest req = new PagoServicioRequest();
                req.setNumeroCuenta(NUMERO_CUENTA);
                req.setPin("1234");
                req.setIdProducto(101);
                req.setIdServicio(50);
                req.setTelefonoReferencia("5512345678");
                req.setMonto(new BigDecimal("100.00"));
                return req;
        }

        private Cuenta crearCuenta(Long idCliente) {
                Cliente cl = Cliente.builder().id(idCliente).build();
                return Cuenta.builder().id(10L).numeroCuenta(NUMERO_CUENTA).cliente(cl).saldo(new BigDecimal("500.00"))
                                .build();
        }

        private TransaccionServicio crearTx(Cuenta c, EstatusTransaccionServicio estatus) {
                return TransaccionServicio.builder()
                                .id(1001L)
                                .cuenta(c)
                                .upc("GP0000000001")
                                .estatus(estatus)
                                .monto(new BigDecimal("100.00"))
                                .saldoPosterior(new BigDecimal("400.00"))
                                .idTxGestopago("GP-TX-999")
                                .numeroAutorizacion("AUTH-12345")
                                .build();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // REALIZAR PAGO - FLUJOS EXITOSOS
        // ─────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("realizarPagoServicio: flujo exitoso para recarga telefónica (tipoFront=1)")
        void realizarPago_exitoso_tipoFront1() {
                PagoServicioRequest request = crearRequest();
                Cuenta cuenta = crearCuenta(1L);
                Usuario usuario = Usuario.builder().id(5L).correo(CORREO).cliente(cuenta.getCliente()).build();

                when(autorizacionOperacionService.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO)).thenReturn(cuenta);
                when(transaccionRepository.findByCuentaIdAndIdempotencyKey(cuenta.getId(), IDEMPOTENCY_KEY))
                                .thenReturn(Optional.empty());

                ValidacionMontoResult montos = new ValidacionMontoResult(
                                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"), 1);
                ProductoItem producto = new ProductoItem();
                producto.setIdProducto(101);
                producto.setIdServicio(50);
                producto.setTipoFront(1);

                when(catalogoPagoValidator.validarOperacion(101, 50, new BigDecimal("100.00"), "5512345678"))
                                .thenReturn(montos);
                when(catalogoPagoValidator.obtenerYValidarProducto(101, 50)).thenReturn(producto);
                when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

                TransaccionServicio tx = crearTx(cuenta, EstatusTransaccionServicio.PENDIENTE);
                when(cuentaDebitoService.reservarFondosYRegistrar(any(), any(), any(), any(), any(), any(), any(),
                                any(), any()))
                                .thenReturn(tx);

                GestoPagoTxResponseXml respXml = new GestoPagoTxResponseXml();
                respXml.setCodigo("01");
                respXml.setIdTx("TX-12345");
                respXml.setNumAutorizacion("AUTH-999");
                respXml.setTexto("Aprobada");

                when(gestoPagoTxGateway.enviarTransaccion(any(GestoPagoSendTxPayload.class), anyString(),
                                eq(tx.getId())))
                                .thenReturn(respXml);

                PagoServicioResponse response = pagoServicioService.realizarPagoServicio(request, CORREO,
                                IDEMPOTENCY_KEY);

                assertNotNull(response);
                assertEquals("EXITOSA", response.getEstatus());
                assertTrue(response.getUpc().matches("^\\d{10}$"));
                assertEquals("TX-12345", response.getIdTxGestoPago());
                assertEquals("AUTH-999", response.getNumeroAutorizacion());

                verify(autorizacionOperacionService).validarPinTransaccional(cuenta, "1234");
                verify(cuentaDebitoService).confirmarExito(tx, respXml);
                ArgumentCaptor<NotificacionTransaccionData> mailCaptor = ArgumentCaptor
                                .forClass(NotificacionTransaccionData.class);
                verify(notificacionTransaccionService).notificarResultadoTransaccion(mailCaptor.capture());
                assertTrue(mailCaptor.getValue().exitosa());
                assertEquals(CORREO, mailCaptor.getValue().correoDestino());
        }

        @Test
        @DisplayName("realizarPagoServicio: genera UPC numérico de 10 dígitos y reintenta si ya existe")
        void realizarPago_generaUpcDe10DigitosYReintentaSiExiste() {
                PagoServicioRequest request = crearRequest();
                Cuenta cuenta = crearCuenta(1L);

                when(autorizacionOperacionService.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO)).thenReturn(cuenta);
                when(transaccionRepository.findByCuentaIdAndIdempotencyKey(cuenta.getId(), IDEMPOTENCY_KEY))
                                .thenReturn(Optional.empty());
                when(transaccionRepository.existsByUpc(anyString())).thenReturn(true, false);

                ValidacionMontoResult montos = new ValidacionMontoResult(
                                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"), 1);
                ProductoItem producto = new ProductoItem();
                producto.setIdProducto(101);
                producto.setIdServicio(50);
                producto.setTipoFront(1);

                when(catalogoPagoValidator.validarOperacion(any(), any(), any(), any())).thenReturn(montos);
                when(catalogoPagoValidator.obtenerYValidarProducto(any(), any())).thenReturn(producto);

                TransaccionServicio tx = crearTx(cuenta, EstatusTransaccionServicio.PENDIENTE);
                when(cuentaDebitoService.reservarFondosYRegistrar(any(), any(), any(), any(), any(), any(), any(),
                                any(), any()))
                                .thenReturn(tx);

                GestoPagoTxResponseXml respXml = new GestoPagoTxResponseXml();
                respXml.setCodigo("01");
                respXml.setIdTx("TX-12345");
                respXml.setNumAutorizacion("AUTH-999");

                when(gestoPagoTxGateway.enviarTransaccion(any(GestoPagoSendTxPayload.class), anyString(),
                                eq(tx.getId())))
                                .thenReturn(respXml);

                PagoServicioResponse response = pagoServicioService.realizarPagoServicio(request, CORREO,
                                IDEMPOTENCY_KEY);

                ArgumentCaptor<GestoPagoSendTxPayload> payloadCaptor = ArgumentCaptor
                                .forClass(GestoPagoSendTxPayload.class);
                verify(gestoPagoTxGateway).enviarTransaccion(payloadCaptor.capture(), eq(response.getUpc()),
                                eq(tx.getId()));

                String upcGenerado = payloadCaptor.getValue().getUpc();
                assertNotNull(upcGenerado);
                assertTrue(upcGenerado.matches("^\\d{10}$"));
                assertEquals(upcGenerado, response.getUpc());
                verify(transaccionRepository, times(2)).existsByUpc(anyString());
        }

        @Test
        @DisplayName("realizarPagoServicio: flujo exitoso para pago de servicio (tipoFront=2)")
        void realizarPago_exitoso_tipoFront2() {
                PagoServicioRequest request = crearRequest();
                Cuenta cuenta = crearCuenta(1L);

                when(autorizacionOperacionService.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO)).thenReturn(cuenta);
                when(transaccionRepository.findByCuentaIdAndIdempotencyKey(cuenta.getId(), IDEMPOTENCY_KEY))
                                .thenReturn(Optional.empty());

                ValidacionMontoResult montos = new ValidacionMontoResult(
                                new BigDecimal("100.00"), new BigDecimal("12.00"), new BigDecimal("112.00"), 2);
                ProductoItem producto = new ProductoItem();
                producto.setIdProducto(101);
                producto.setIdServicio(50);
                producto.setTipoFront(2);

                when(catalogoPagoValidator.validarOperacion(any(), any(), any(), any())).thenReturn(montos);
                when(catalogoPagoValidator.obtenerYValidarProducto(any(), any())).thenReturn(producto);

                TransaccionServicio tx = crearTx(cuenta, EstatusTransaccionServicio.PENDIENTE);
                when(cuentaDebitoService.reservarFondosYRegistrar(any(), any(), any(), any(), any(), any(), any(),
                                any(), any()))
                                .thenReturn(tx);

                GestoPagoTxResponseXml respXml = new GestoPagoTxResponseXml();
                respXml.setCodigo("01");
                respXml.setIdTx("TX-PAGO");
                respXml.setNumAutorizacion("AUTH-PAGO");

                when(gestoPagoTxGateway.enviarTransaccion(any(), any(), any())).thenReturn(respXml);

                PagoServicioResponse response = pagoServicioService.realizarPagoServicio(request, CORREO,
                                IDEMPOTENCY_KEY);

                assertNotNull(response);
                assertEquals("EXITOSA", response.getEstatus());
                assertEquals(new BigDecimal("112.00"), response.getMontoDebitado());
        }

        // ─────────────────────────────────────────────────────────────────────────
        // IDEMPOTENCIA
        // ─────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("realizarPagoServicio: falta Idempotency-Key lanza ReglaNegocioException")
        void realizarPago_faltaIdempotencyKey() {
                PagoServicioRequest request = crearRequest();
                Cuenta cuenta = crearCuenta(1L);
                when(autorizacionOperacionService.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO)).thenReturn(cuenta);

                ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                                () -> pagoServicioService.realizarPagoServicio(request, CORREO, "   "));
                assertTrue(ex.getMessage().contains("Idempotency-Key"));
        }

        @Test
        @DisplayName("realizarPagoServicio: conflicto de idempotencia por parámetros distintos")
        void realizarPago_idempotenciaConflictoParametros() {
                PagoServicioRequest request = crearRequest();
                Cuenta cuenta = crearCuenta(1L);
                when(autorizacionOperacionService.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO)).thenReturn(cuenta);

                TransaccionServicio previa = crearTx(cuenta, EstatusTransaccionServicio.EXITOSA);
                previa.setRequestHash("HASH_DISTINTO_PREVIO");

                when(transaccionRepository.findByCuentaIdAndIdempotencyKey(cuenta.getId(), IDEMPOTENCY_KEY))
                                .thenReturn(Optional.of(previa));

                assertThrows(IdempotenciaConflictoException.class,
                                () -> pagoServicioService.realizarPagoServicio(request, CORREO, IDEMPOTENCY_KEY));
        }

        @Test
        @DisplayName("realizarPagoServicio: conflicto de idempotencia cuando la transacción previa aún está PENDIENTE")
        void realizarPago_idempotenciaTransaccionEnProceso() {
                PagoServicioRequest request = crearRequest();
                Cuenta cuenta = crearCuenta(1L);
                when(autorizacionOperacionService.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO)).thenReturn(cuenta);

                String hash = "f16016da0cfc4bdf45ae86cfb9e1180aeeb40986638b971a80c95ef91276a6b8"; // Hash calculado para
                                                                                                  // request
                TransaccionServicio previa = crearTx(cuenta, EstatusTransaccionServicio.PENDIENTE);
                previa.setRequestHash(hash);

                // Forzar cálculo de hash idéntico mockeando el comportamiento
                when(transaccionRepository.findByCuentaIdAndIdempotencyKey(cuenta.getId(), IDEMPOTENCY_KEY))
                                .thenAnswer(inv -> {
                                        // Ponemos el hash que generará
                                        String h = ReflectionTestUtils.invokeMethod(pagoServicioService,
                                                        "calcularSha256",
                                                        request.getNumeroCuenta() + "|" + request.getIdProducto() + "|"
                                                                        +
                                                                        request.getIdServicio() + "|"
                                                                        + request.getTelefonoReferencia() + "|" +
                                                                        request.getMonto().toPlainString());
                                        previa.setRequestHash(h);
                                        return Optional.of(previa);
                                });

                IdempotenciaConflictoException ex = assertThrows(IdempotenciaConflictoException.class,
                                () -> pagoServicioService.realizarPagoServicio(request, CORREO, IDEMPOTENCY_KEY));
                assertTrue(ex.getMessage().contains("actualmente en proceso"));
        }

        @Test
        @DisplayName("realizarPagoServicio: idempotente exitoso previo retorna respuesta previa sin aplicar nuevo cargo")
        void realizarPago_idempotenteExitoso_retornaPrevio() {
                PagoServicioRequest request = crearRequest();
                Cuenta cuenta = crearCuenta(1L);
                when(autorizacionOperacionService.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO)).thenReturn(cuenta);

                TransaccionServicio previa = crearTx(cuenta, EstatusTransaccionServicio.EXITOSA);

                when(transaccionRepository.findByCuentaIdAndIdempotencyKey(cuenta.getId(), IDEMPOTENCY_KEY))
                                .thenAnswer(inv -> {
                                        String h = ReflectionTestUtils.invokeMethod(pagoServicioService,
                                                        "calcularSha256",
                                                        request.getNumeroCuenta() + "|" + request.getIdProducto() + "|"
                                                                        +
                                                                        request.getIdServicio() + "|"
                                                                        + request.getTelefonoReferencia() + "|" +
                                                                        request.getMonto().toPlainString());
                                        previa.setRequestHash(h);
                                        return Optional.of(previa);
                                });

                PagoServicioResponse response = pagoServicioService.realizarPagoServicio(request, CORREO,
                                IDEMPOTENCY_KEY);

                assertNotNull(response);
                assertEquals(previa.getId(), response.getIdTransaccion());
                assertEquals("EXITOSA", response.getEstatus());

                verifyNoInteractions(cuentaDebitoService);
                verifyNoInteractions(gestoPagoTxGateway);
        }

        // ─────────────────────────────────────────────────────────────────────────
        // MANEJO DE ERRORES Y PROVEEDOR
        // ─────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("realizarPagoServicio: proveedor rechaza transacción, reembolsa saldo y lanza ProveedorException")
        void realizarPago_proveedorRechaza_reembolsa() {
                PagoServicioRequest request = crearRequest();
                Cuenta cuenta = crearCuenta(1L);

                when(autorizacionOperacionService.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO)).thenReturn(cuenta);
                when(transaccionRepository.findByCuentaIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());

                ValidacionMontoResult montos = new ValidacionMontoResult(new BigDecimal("100.00"), BigDecimal.ZERO,
                                new BigDecimal("100.00"), 1);
                ProductoItem producto = new ProductoItem();
                producto.setTipoFront(1);

                when(catalogoPagoValidator.validarOperacion(any(), any(), any(), any())).thenReturn(montos);
                when(catalogoPagoValidator.obtenerYValidarProducto(any(), any())).thenReturn(producto);

                TransaccionServicio tx = crearTx(cuenta, EstatusTransaccionServicio.PENDIENTE);
                when(cuentaDebitoService.reservarFondosYRegistrar(any(), any(), any(), any(), any(), any(), any(),
                                any(), any()))
                                .thenReturn(tx);

                GestoPagoTxResponseXml rechazoXml = new GestoPagoTxResponseXml();
                rechazoXml.setCodigo("99");
                rechazoXml.setTexto("Saldo insuficiente de la pasarela");

                when(gestoPagoTxGateway.enviarTransaccion(any(), any(), any())).thenReturn(rechazoXml);

                ProveedorException ex = assertThrows(ProveedorException.class,
                                () -> pagoServicioService.realizarPagoServicio(request, CORREO, IDEMPOTENCY_KEY));

                assertEquals("99", ex.getCodigoRespuesta());
                verify(cuentaDebitoService).registrarFalloYReembolsar(tx, rechazoXml,
                                "Saldo insuficiente de la pasarela");
                ArgumentCaptor<NotificacionTransaccionData> mailCaptor = ArgumentCaptor
                                .forClass(NotificacionTransaccionData.class);
                verify(notificacionTransaccionService).notificarResultadoTransaccion(mailCaptor.capture());
                assertFalse(mailCaptor.getValue().exitosa());
                assertEquals("Saldo insuficiente de la pasarela", mailCaptor.getValue().motivo());
        }

        @Test
        @DisplayName("realizarPagoServicio: rechazo código 55 con validIPS sanitiza el motivo sin filtrar lista de IPs")
        void realizarPago_proveedorCodigo55ValidIps_sanitizaMotivoSinFiltrarListaIps() {
                PagoServicioRequest request = crearRequest();
                Cuenta cuenta = crearCuenta(1L);

                when(autorizacionOperacionService.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO)).thenReturn(cuenta);
                when(transaccionRepository.findByCuentaIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());

                ValidacionMontoResult montos = new ValidacionMontoResult(new BigDecimal("100.00"), BigDecimal.ZERO,
                                new BigDecimal("100.00"), 1);
                ProductoItem producto = new ProductoItem();
                producto.setTipoFront(1);

                when(catalogoPagoValidator.validarOperacion(any(), any(), any(), any())).thenReturn(montos);
                when(catalogoPagoValidator.obtenerYValidarProducto(any(), any())).thenReturn(producto);

                TransaccionServicio tx = crearTx(cuenta, EstatusTransaccionServicio.PENDIENTE);
                when(cuentaDebitoService.reservarFondosYRegistrar(any(), any(), any(), any(), any(), any(), any(),
                                any(), any()))
                                .thenReturn(tx);

                GestoPagoTxResponseXml rechazoXml = new GestoPagoTxResponseXml();
                rechazoXml.setCodigo("55");
                rechazoXml.setTexto("{validationCompleted=false, codigoDispositivo=GPS83-TPV-17, IP=187.140.67.84, isValid=false, validIPS=(83): [163.192.134.186],[44.205.98.58]}");

                when(gestoPagoTxGateway.enviarTransaccion(any(), any(), any())).thenReturn(rechazoXml);

                ProveedorException ex = assertThrows(ProveedorException.class,
                                () -> pagoServicioService.realizarPagoServicio(request, CORREO, IDEMPOTENCY_KEY));

                assertEquals("55", ex.getCodigoRespuesta());
                assertEquals("Transacción rechazada por el proveedor (Código: 55)", ex.getMessage());
                assertFalse(ex.getMessage().contains("187.140.67.84"));
                assertFalse(ex.getMessage().contains("IP"));
                assertFalse(ex.getMessage().contains("validIPS"));
                verify(cuentaDebitoService).registrarFalloYReembolsar(eq(tx), eq(rechazoXml),
                                eq("Transacción rechazada por el proveedor"));
        }

        @Test
        @DisplayName("realizarPagoServicio: timeout/resultado incierto envía transacción a conciliación")
        void realizarPago_resultadoIncierto_enviaAConciliacion() {
                PagoServicioRequest request = crearRequest();
                Cuenta cuenta = crearCuenta(1L);

                when(autorizacionOperacionService.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO)).thenReturn(cuenta);
                when(transaccionRepository.findByCuentaIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());

                ValidacionMontoResult montos = new ValidacionMontoResult(new BigDecimal("100.00"), BigDecimal.ZERO,
                                new BigDecimal("100.00"), 1);
                ProductoItem producto = new ProductoItem();
                producto.setTipoFront(1);

                when(catalogoPagoValidator.validarOperacion(any(), any(), any(), any())).thenReturn(montos);
                when(catalogoPagoValidator.obtenerYValidarProducto(any(), any())).thenReturn(producto);

                TransaccionServicio tx = crearTx(cuenta, EstatusTransaccionServicio.PENDIENTE);
                when(cuentaDebitoService.reservarFondosYRegistrar(any(), any(), any(), any(), any(), any(), any(),
                                any(), any()))
                                .thenReturn(tx);

                when(gestoPagoTxGateway.enviarTransaccion(any(), any(), any()))
                                .thenThrow(new ResultadoInciertoException("Timeout del proveedor", tx));

                assertThrows(ResultadoInciertoException.class,
                                () -> pagoServicioService.realizarPagoServicio(request, CORREO, IDEMPOTENCY_KEY));

                verify(cuentaDebitoService).registrarEnConciliacion(tx, "82", "Timeout del proveedor");
        }

        @Test
        @DisplayName("realizarPagoServicio: excepción genérica de red envía a conciliación y lanza ResultadoInciertoException")
        void realizarPago_excepcionGenerica_enviaAConciliacion() {
                PagoServicioRequest request = crearRequest();
                Cuenta cuenta = crearCuenta(1L);

                when(autorizacionOperacionService.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO)).thenReturn(cuenta);
                when(transaccionRepository.findByCuentaIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());

                ValidacionMontoResult montos = new ValidacionMontoResult(new BigDecimal("100.00"), BigDecimal.ZERO,
                                new BigDecimal("100.00"), 1);
                ProductoItem producto = new ProductoItem();
                producto.setTipoFront(1);

                when(catalogoPagoValidator.validarOperacion(any(), any(), any(), any())).thenReturn(montos);
                when(catalogoPagoValidator.obtenerYValidarProducto(any(), any())).thenReturn(producto);

                TransaccionServicio tx = crearTx(cuenta, EstatusTransaccionServicio.PENDIENTE);
                when(cuentaDebitoService.reservarFondosYRegistrar(any(), any(), any(), any(), any(), any(), any(),
                                any(), any()))
                                .thenReturn(tx);

                when(gestoPagoTxGateway.enviarTransaccion(any(), any(), any()))
                                .thenThrow(new RuntimeException("Connection reset by peer"));

                assertThrows(ResultadoInciertoException.class,
                                () -> pagoServicioService.realizarPagoServicio(request, CORREO, IDEMPOTENCY_KEY));

                verify(cuentaDebitoService).registrarEnConciliacion(tx, "ERROR", "Connection reset by peer");
        }

        // ─────────────────────────────────────────────────────────────────────────
        // CONSULTAR ESTADO
        // ─────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("consultarEstadoTransaccion: transacción existente y del titular retorna respuesta")
        void consultarEstadoTransaccion_exitosa() {
                Cuenta cuenta = crearCuenta(1L);
                Usuario usuario = Usuario.builder().id(5L).correo(CORREO).cliente(cuenta.getCliente()).build();
                TransaccionServicio tx = crearTx(cuenta, EstatusTransaccionServicio.EXITOSA);

                when(transaccionRepository.findById(1001L)).thenReturn(Optional.of(tx));
                when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

                PagoServicioResponse response = pagoServicioService.consultarEstadoTransaccion(1001L, CORREO);

                assertNotNull(response);
                assertEquals(1001L, response.getIdTransaccion());
                assertEquals("EXITOSA", response.getEstatus());
        }

        @Test
        @DisplayName("consultarEstadoTransaccion: transacción no encontrada lanza RecursoNoEncontradoException")
        void consultarEstadoTransaccion_noExiste() {
                when(transaccionRepository.findById(9999L)).thenReturn(Optional.empty());

                assertThrows(RecursoNoEncontradoException.class,
                                () -> pagoServicioService.consultarEstadoTransaccion(9999L, CORREO));
        }

        @Test
        @DisplayName("consultarEstadoTransaccion: intento de consultar transacción de otro cliente lanza RecursoNoEncontradoException")
        void consultarEstadoTransaccion_cuentaAjena() {
                Cuenta cuenta = crearCuenta(1L);
                Cliente otroCliente = Cliente.builder().id(99L).build();
                Usuario usuarioIntruso = Usuario.builder().id(5L).correo(CORREO).cliente(otroCliente).build();
                TransaccionServicio tx = crearTx(cuenta, EstatusTransaccionServicio.EXITOSA);

                when(transaccionRepository.findById(1001L)).thenReturn(Optional.of(tx));
                when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuarioIntruso));

                assertThrows(RecursoNoEncontradoException.class,
                                () -> pagoServicioService.consultarEstadoTransaccion(1001L, CORREO));
        }
}
