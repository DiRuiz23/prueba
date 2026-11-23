package com.proyecto.servicios.service.gestopago.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.client.model.GestoPagoSendTxPayload;
import com.proyecto.servicios.client.model.GestoPagoTxResponseXml;
import com.proyecto.servicios.entity.banco.GestoPagoBitacora;
import com.proyecto.servicios.exception.gestopago.ProveedorNoDisponibleException;
import com.proyecto.servicios.exception.gestopago.ResultadoInciertoException;
import com.proyecto.servicios.repository.banco.GestoPagoBitacoraRepository;
import com.proyecto.servicios.service.gestopago.GestoPagoTokenService;
import com.proyecto.servicios.util.AesCifradoUtil;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: FeignGestoPagoTxGateway")
class FeignGestoPagoTxGatewayTest {

    private static final String VALID_XML = """
            <RESPONSE>
                <ID_TX>TX-100</ID_TX>
                <NUM_AUTORIZACION>AUTH-001</NUM_AUTORIZACION>
                <MENSAJE>
                    <CODIGO>01</CODIGO>
                    <TEXTO>Exitosa</TEXTO>
                </MENSAJE>
            </RESPONSE>
            """;

    @Mock
    private GestoPagoTxClient gestoPagoTxClient;

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private AesCifradoUtil aesCifradoUtil;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private GestoPagoBitacoraRepository bitacoraRepository;

    @InjectMocks
    private FeignGestoPagoTxGateway gateway;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gateway, "apiKey", "test-api-key");
    }

    private GestoPagoSendTxPayload crearPayload() {
        return GestoPagoSendTxPayload.builder()
                .idProducto(101)
                .idServicio(50)
                .horaLocal("20261109120000")
                .upc("GP0000000001")
                .unidad("083")
                .telefono("5512345678")
                .build();
    }

    private FeignException.Unauthorized crearUnauthorizedException() {
        Request request = Request.create(Request.HttpMethod.POST, "/tx", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.Unauthorized("Token expirado", request, null, Collections.emptyMap());
    }

    private FeignException.GatewayTimeout crearGatewayTimeoutException() {
        Request request = Request.create(Request.HttpMethod.POST, "/tx", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.GatewayTimeout("Gateway timeout", request, null, Collections.emptyMap());
    }

    @Test
    @DisplayName("enviarTransaccion: llamada exitosa sendTx retorna objeto XML parseado y guarda bitácora")
    void enviarTransaccion_exitosa() {
        GestoPagoSendTxPayload payload = crearPayload();
        when(aesCifradoUtil.cifrar(anyString())).thenReturn("signed_payload");
        when(tokenService.obtenerTokenBearerValido()).thenReturn("token_valido");
        when(gestoPagoTxClient.sendTx(eq("Bearer token_valido"), eq("test-api-key"), anyString(), eq("signed_payload")))
                .thenReturn(VALID_XML);

        GestoPagoTxResponseXml response = gateway.enviarTransaccion(payload, "GP0000000001", 1001L);

        assertNotNull(response);
        assertEquals("01", response.getCodigo());
        assertEquals("TX-100", response.getIdTx());
        assertTrue(response.esExitosa());

        verify(bitacoraRepository).save(any(GestoPagoBitacora.class));
    }

    @Test
    @DisplayName("consultarTransaccion: llamada exitosa confirmTx consulta estado")
    void consultarTransaccion_exitosa() {
        GestoPagoSendTxPayload payload = crearPayload();
        when(aesCifradoUtil.cifrar(anyString())).thenReturn("signed_payload");
        when(tokenService.obtenerTokenBearerValido()).thenReturn("token_valido");
        when(gestoPagoTxClient.confirmTx(eq("Bearer token_valido"), eq("test-api-key"), anyString(), eq("signed_payload")))
                .thenReturn(VALID_XML);

        GestoPagoTxResponseXml response = gateway.consultarTransaccion(payload, "GP0000000001", 1001L);

        assertNotNull(response);
        assertEquals("01", response.getCodigo());
        verify(gestoPagoTxClient).confirmTx(any(), any(), any(), any());
    }

    @Test
    @DisplayName("enviarTransaccion: error 401 Unauthorized ejecuta refresh-once de token y reintenta con éxito")
    void enviarTransaccion_tokenExpirado_refreshOnceExitoso() {
        GestoPagoSendTxPayload payload = crearPayload();
        when(aesCifradoUtil.cifrar(anyString())).thenReturn("signed");
        when(tokenService.obtenerTokenBearerValido()).thenReturn("token_expirado");

        when(gestoPagoTxClient.sendTx(eq("Bearer token_expirado"), any(), any(), any()))
                .thenThrow(crearUnauthorizedException());

        when(tokenService.forzarRenovacionYObtenerToken()).thenReturn("token_nuevo");
        when(gestoPagoTxClient.sendTx(eq("Bearer token_nuevo"), any(), any(), any()))
                .thenReturn(VALID_XML);

        GestoPagoTxResponseXml response = gateway.enviarTransaccion(payload, "GP0000000001", 1001L);

        assertNotNull(response);
        assertEquals("01", response.getCodigo());
        verify(tokenService).forzarRenovacionYObtenerToken();
    }

    @Test
    @DisplayName("enviarTransaccion: error 401 y falla en renovación de token lanza ProveedorNoDisponibleException")
    void enviarTransaccion_tokenExpirado_fallaRenovacion() {
        GestoPagoSendTxPayload payload = crearPayload();
        when(aesCifradoUtil.cifrar(anyString())).thenReturn("signed");
        when(tokenService.obtenerTokenBearerValido()).thenReturn("token_expirado");

        when(gestoPagoTxClient.sendTx(eq("Bearer token_expirado"), any(), any(), any()))
                .thenThrow(crearUnauthorizedException());

        when(tokenService.forzarRenovacionYObtenerToken()).thenThrow(new RuntimeException("Auth server down"));

        assertThrows(ProveedorNoDisponibleException.class,
                () -> gateway.enviarTransaccion(payload, "GP0000000001", 1001L));

        verify(bitacoraRepository).save(any(GestoPagoBitacora.class));
    }

    @Test
    @DisplayName("enviarTransaccion: error Feign 5xx del proveedor lanza ResultadoInciertoException")
    void enviarTransaccion_errorFeign5xx_resultadoIncierto() {
        GestoPagoSendTxPayload payload = crearPayload();
        when(aesCifradoUtil.cifrar(anyString())).thenReturn("signed");
        when(tokenService.obtenerTokenBearerValido()).thenReturn("token_valido");

        when(gestoPagoTxClient.sendTx(any(), any(), any(), any()))
                .thenThrow(crearGatewayTimeoutException());

        assertThrows(ResultadoInciertoException.class,
                () -> gateway.enviarTransaccion(payload, "GP0000000001", 1001L));

        verify(bitacoraRepository).save(any(GestoPagoBitacora.class));
    }

    @Test
    @DisplayName("enviarTransaccion: excepción genérica de red lanza ResultadoInciertoException")
    void enviarTransaccion_errorRed_resultadoIncierto() {
        GestoPagoSendTxPayload payload = crearPayload();
        when(aesCifradoUtil.cifrar(anyString())).thenReturn("signed");
        when(tokenService.obtenerTokenBearerValido()).thenReturn("token_valido");

        when(gestoPagoTxClient.sendTx(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Connection timed out"));

        assertThrows(ResultadoInciertoException.class,
                () -> gateway.enviarTransaccion(payload, "GP0000000001", 1001L));
    }

    @Test
    @DisplayName("enviarTransaccion: proveedor responde código 82 (timeout pasarela) en sendTx lanza ResultadoInciertoException")
    void enviarTransaccion_codigo82_resultadoIncierto() {
        GestoPagoSendTxPayload payload = crearPayload();
        String xml82 = """
                <RESPONSE>
                    <ID_TX>TX-82</ID_TX>
                    <MENSAJE>
                        <CODIGO>82</CODIGO>
                        <TEXTO>Tiempo agotado en pasarela</TEXTO>
                    </MENSAJE>
                </RESPONSE>
                """;

        when(aesCifradoUtil.cifrar(anyString())).thenReturn("signed");
        when(tokenService.obtenerTokenBearerValido()).thenReturn("token_valido");
        when(gestoPagoTxClient.sendTx(any(), any(), any(), any())).thenReturn(xml82);

        ResultadoInciertoException ex = assertThrows(ResultadoInciertoException.class,
                () -> gateway.enviarTransaccion(payload, "GP0000000001", 1001L));
        assertTrue(ex.getMessage().contains("código 82"));
    }

    @Test
    @DisplayName("enviarTransaccion: proveedor responde código 06 en sendTx lanza ResultadoInciertoException")
    void enviarTransaccion_codigo06_resultadoIncierto() {
        GestoPagoSendTxPayload payload = crearPayload();
        String xml06 = """
                <RESPONSE>
                    <ID_TX>TX-06</ID_TX>
                    <MENSAJE>
                        <CODIGO>06</CODIGO>
                        <TEXTO>Posible duplicidad / En proceso</TEXTO>
                    </MENSAJE>
                </RESPONSE>
                """;

        when(aesCifradoUtil.cifrar(anyString())).thenReturn("signed");
        when(tokenService.obtenerTokenBearerValido()).thenReturn("token_valido");
        when(gestoPagoTxClient.sendTx(any(), any(), any(), any())).thenReturn(xml06);

        ResultadoInciertoException ex = assertThrows(ResultadoInciertoException.class,
                () -> gateway.enviarTransaccion(payload, "GP0000000001", 1001L));
        assertTrue(ex.getMessage().contains("código 06"));
    }

    @Test
    @DisplayName("enviarTransaccion: respuesta XML vacía o malformada lanza ResultadoInciertoException")
    void enviarTransaccion_xmlMalformado_resultadoIncierto() {
        GestoPagoSendTxPayload payload = crearPayload();
        when(aesCifradoUtil.cifrar(anyString())).thenReturn("signed");
        when(tokenService.obtenerTokenBearerValido()).thenReturn("token_valido");
        when(gestoPagoTxClient.sendTx(any(), any(), any(), any())).thenReturn("RESPUESTA_NO_XML");

        assertThrows(ResultadoInciertoException.class,
                () -> gateway.enviarTransaccion(payload, "GP0000000001", 1001L));

        verify(bitacoraRepository).save(any(GestoPagoBitacora.class));
    }
}

