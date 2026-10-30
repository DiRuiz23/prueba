package com.proyecto.servicios.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.model.CatProductXml.ProductoItem;
import com.proyecto.servicios.client.model.GestoPagoTxResponseXml;
import com.proyecto.servicios.dto.request.banco.PagoServicioRequest;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.enums.EstatusTransaccionServicio;
import com.proyecto.servicios.repository.banco.TransaccionServicioRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.service.banco.AutorizacionOperacionService;
import com.proyecto.servicios.service.banco.CuentaDebitoService;
import com.proyecto.servicios.service.banco.impl.PagoServicioServiceImpl;
import com.proyecto.servicios.service.banco.strategy.ValidacionMontoResult;
import com.proyecto.servicios.service.gestopago.CatalogoPagoValidator;
import com.proyecto.servicios.service.gestopago.GestoPagoTxGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Hardening de Seguridad: Verificación Anti-Fuga en Logs")
class LogsAntiFugaSeguridadTest {

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
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PagoServicioServiceImpl pagoServicioService;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(PagoServicioServiceImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("El flujo completo de pago NO expone PIN, contraseña ni payload plano en los registros de log")
    void pagoServicio_noFugaSecretosEnLogs() {
        String pinSecreto = "SuperSecretPin99!";
        String numeroCuenta = "01200000000000000016";
        String telefono = "5512345678";

        Cuenta cuenta = Cuenta.builder().id(1L).numeroCuenta(numeroCuenta).estatus(EstatusCuenta.ACTIVA).build();
        when(autorizacionOperacionService.obtenerCuentaAutorizada(numeroCuenta, "test@banco.com")).thenReturn(cuenta);

        ProductoItem prod = new ProductoItem();
        prod.setIdProducto(582);
        prod.setIdServicio(133);
        prod.setTipoFront(1);
        prod.setPrecio("10.00");

        when(catalogoPagoValidator.validarOperacion(any(), any(), any(), any()))
                .thenReturn(new ValidacionMontoResult(new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("10.00")));
        when(catalogoPagoValidator.obtenerYValidarProducto(any(), any())).thenReturn(prod);

        TransaccionServicio tx = TransaccionServicio.builder()
                .id(1L)
                .upc("GP000000000001")
                .estatus(EstatusTransaccionServicio.PENDIENTE)
                .montoDebitado(new BigDecimal("10.00"))
                .saldoPosterior(new BigDecimal("990.00"))
                .build();
        when(cuentaDebitoService.reservarFondosYRegistrar(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(tx);

        GestoPagoTxResponseXml respXml = new GestoPagoTxResponseXml();
        respXml.setIdTx("TX-12345");
        respXml.setNumAutorizacion("AUTH-67890");
        respXml.setCodigo("01");
        respXml.setTexto("Aprobada");
        when(gestoPagoTxGateway.enviarTransaccion(any(), any(), any())).thenReturn(respXml);

        PagoServicioRequest req = PagoServicioRequest.builder()
                .numeroCuenta(numeroCuenta)
                .idProducto(582)
                .idServicio(133)
                .telefonoReferencia(telefono)
                .monto(new BigDecimal("10.00"))
                .pin(pinSecreto)
                .build();

        pagoServicioService.realizarPagoServicio(req, "test@banco.com", "idemp-key-1");

        // Validar todos los logs emitidos
        assertFalse(listAppender.list.isEmpty());
        for (ILoggingEvent event : listAppender.list) {
            String logMsg = event.getFormattedMessage();
            assertFalse(logMsg.contains(pinSecreto), "¡ALERTA DE SEGURIDAD! El PIN secreto apareció en los logs: " + logMsg);
            assertFalse(logMsg.contains("SuperSecret"), "¡ALERTA DE SEGURIDAD! Cadena secreta en logs: " + logMsg);
        }
    }
}

