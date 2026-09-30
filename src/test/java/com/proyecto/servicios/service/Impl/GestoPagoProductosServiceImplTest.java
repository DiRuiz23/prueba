package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductosClient;
import com.proyecto.servicios.entity.gestopago.CatProductDocument;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.CatProductXml;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.repositorys.gestopago.CatProductRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link GestoPagoProductosServiceImpl}.
 *
 * Cubre los flujos principales:
 * <ul>
 *   <li>Obtención exitosa del catálogo desde MongoDB</li>
 *   <li>Catálogo no disponible en MongoDB</li>
 *   <li>Actualización exitosa del catálogo desde GestoPago</li>
 *   <li>Token no encontrado al actualizar catálogo</li>
 *   <li>Error de autenticación (401/403) al llamar al servicio externo</li>
 *   <li>Error de comunicación genérico (timeout, red, etc.)</li>
 *   <li>Error HTTP del servicio externo (Feign 5xx)</li>
 *   <li>Respuesta vacía o nula del servicio externo</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class GestoPagoProductosServiceImplTest {

    @Mock
    private GestoPagoProductosClient gestoPagoProductosClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @Mock
    private CatProductRepository catProductRepository;

    @InjectMocks
    private GestoPagoProductosServiceImpl gestoPagoProductosService;

    private static final Integer ID_DISTRIBUIDOR = 83;
    private static final String CODIGO_DISPOSITIVO = "GPS83-TPV-17";
    private static final String TOKEN_VALOR = "token-test-abc123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gestoPagoProductosService, "idDistribuidor", ID_DISTRIBUIDOR);
        ReflectionTestUtils.setField(gestoPagoProductosService, "codigoDispositivo", CODIGO_DISPOSITIVO);
    }

    // =========================================================================
    // obtenerCatalogoProductos — escenarios exitosos
    // =========================================================================

    @Test
    @DisplayName("obtenerCatalogoProductos: retorna catálogo OK cuando existe documento en MongoDB")
    void obtenerCatalogoProductos_cuandoExisteDocumento_retornaRespuestaOk() {
        // Arrange
        CatProductDocument documento = buildDocumento();
        when(catProductRepository.findTopByOrderByFechaActualizacionDesc())
                .thenReturn(Optional.of(documento));

        // Act
        GestoPagoProductListResponse resultado = gestoPagoProductosService.obtenerCatalogoProductos();

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getStatus()).isEqualTo("OK");
        assertThat(resultado.getMensaje()).isEqualTo("Catálogo obtenido correctamente");
        assertThat(resultado.getProducts()).hasSize(1);
        assertThat(resultado.getFechaConsulta()).isNotNull();

        verify(catProductRepository, times(1)).findTopByOrderByFechaActualizacionDesc();
    }

    // =========================================================================
    // obtenerCatalogoProductos — escenarios de error
    // =========================================================================

    @Test
    @DisplayName("obtenerCatalogoProductos: retorna ERROR cuando no hay catálogo en MongoDB")
    void obtenerCatalogoProductos_cuandoNoExisteDocumento_retornaRespuestaError() {
        // Arrange
        when(catProductRepository.findTopByOrderByFechaActualizacionDesc())
                .thenReturn(Optional.empty());

        // Act
        GestoPagoProductListResponse resultado = gestoPagoProductosService.obtenerCatalogoProductos();

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getStatus()).isEqualTo("ERROR");
        assertThat(resultado.getMensaje()).isNotBlank();
        assertThat(resultado.getProducts()).isNull();
    }

    // =========================================================================
    // actualizarCatalogoProductos — escenarios exitosos
    // =========================================================================

    @Test
    @DisplayName("actualizarCatalogoProductos: persiste catálogo cuando token y XML son válidos")
    void actualizarCatalogoProductos_cuandoTokenYXmlValidos_persisteDocumento() {
        // Arrange
        GestoPagoToken token = buildToken();
        when(gestoPagoTokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(token));

        String xmlValido = buildXmlValido();
        when(gestoPagoProductosClient.getProductList("Bearer " + TOKEN_VALOR))
                .thenReturn(xmlValido);

        // Act
        gestoPagoProductosService.actualizarCatalogoProductos();

        // Assert
        verify(catProductRepository, times(1)).save(any(CatProductDocument.class));
        verify(gestoPagoProductosClient, times(1)).getProductList("Bearer " + TOKEN_VALOR);
    }

    // =========================================================================
    // actualizarCatalogoProductos — escenarios de error
    // =========================================================================

    @Test
    @DisplayName("actualizarCatalogoProductos: no invoca cliente cuando no hay token activo")
    void actualizarCatalogoProductos_cuandoNoHayToken_noProcesa() {
        // Arrange
        when(gestoPagoTokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.empty());

        // Act
        gestoPagoProductosService.actualizarCatalogoProductos();

        // Assert
        verify(gestoPagoProductosClient, never()).getProductList(anyString());
        verify(catProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarCatalogoProductos: no guarda cuando la respuesta XML está vacía")
    void actualizarCatalogoProductos_cuandoRespuestaVacia_noPersiste() {
        // Arrange
        GestoPagoToken token = buildToken();
        when(gestoPagoTokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(token));
        when(gestoPagoProductosClient.getProductList(anyString()))
                .thenReturn("");

        // Act
        gestoPagoProductosService.actualizarCatalogoProductos();

        // Assert
        verify(catProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarCatalogoProductos: no guarda cuando la respuesta XML es nula")
    void actualizarCatalogoProductos_cuandoRespuestaNula_noPersiste() {
        // Arrange
        GestoPagoToken token = buildToken();
        when(gestoPagoTokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(token));
        when(gestoPagoProductosClient.getProductList(anyString()))
                .thenReturn(null);

        // Act
        gestoPagoProductosService.actualizarCatalogoProductos();

        // Assert
        verify(catProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarCatalogoProductos: maneja error de autenticación (401) sin propagar excepción")
    void actualizarCatalogoProductos_cuandoError401_manejaExcepcionSinPropagar() {
        // Arrange
        GestoPagoToken token = buildToken();
        when(gestoPagoTokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(token));
        when(gestoPagoProductosClient.getProductList(anyString()))
                .thenThrow(buildFeignException(401));

        // Act — no debe lanzar excepción
        gestoPagoProductosService.actualizarCatalogoProductos();

        // Assert
        verify(catProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarCatalogoProductos: maneja error de comunicación genérico sin propagar excepción")
    void actualizarCatalogoProductos_cuandoErrorComunicacion_manejaExcepcionSinPropagar() {
        // Arrange
        GestoPagoToken token = buildToken();
        when(gestoPagoTokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(token));
        when(gestoPagoProductosClient.getProductList(anyString()))
                .thenThrow(new RuntimeException("Connection refused: gestopago.portalventas.net"));

        // Act — no debe lanzar excepción
        gestoPagoProductosService.actualizarCatalogoProductos();

        // Assert
        verify(catProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarCatalogoProductos: maneja error HTTP 503 del servicio externo sin propagar excepción")
    void actualizarCatalogoProductos_cuandoError503_manejaExcepcionSinPropagar() {
        // Arrange
        GestoPagoToken token = buildToken();
        when(gestoPagoTokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(token));
        when(gestoPagoProductosClient.getProductList(anyString()))
                .thenThrow(buildFeignException(503));

        // Act — no debe lanzar excepción
        gestoPagoProductosService.actualizarCatalogoProductos();

        // Assert
        verify(catProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarCatalogoProductos: construye el Bearer Token correctamente")
    void actualizarCatalogoProductos_verificaFormatoBearerToken() {
        // Arrange
        GestoPagoToken token = buildToken();
        when(gestoPagoTokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(token));
        when(gestoPagoProductosClient.getProductList(anyString()))
                .thenReturn(buildXmlValido());

        // Act
        gestoPagoProductosService.actualizarCatalogoProductos();

        // Assert — verificar que se envía exactamente "Bearer <token>"
        verify(gestoPagoProductosClient).getProductList("Bearer " + TOKEN_VALOR);
    }

    // =========================================================================
    // Métodos auxiliares de construcción
    // =========================================================================

    private GestoPagoToken buildToken() {
        GestoPagoToken token = new GestoPagoToken();
        token.setId(1);
        token.setToken(TOKEN_VALOR);
        token.setIdDistribuidor(ID_DISTRIBUIDOR);
        token.setCodigoDispositivo(CODIGO_DISPOSITIVO);
        token.setActivo(true);
        return token;
    }

    private CatProductDocument buildDocumento() {
        CatProductDocument doc = new CatProductDocument();
        doc.setId("doc-001");
        doc.setStatus("OK");
        doc.setFuente("GESTOPAGO");
        doc.setFechaActualizacion(LocalDateTime.now());
        doc.setFechaCreacion(LocalDateTime.now());

        CatProductXml.ProductoItem item = new CatProductXml.ProductoItem();
        item.setIdProducto(1001);
        item.setProducto("RECARGA TELCEL");
        item.setIdServicio(10);
        item.setServicio("TELCEL");
        item.setPrecio("100.00");

        doc.setProducts(List.of(item));
        return doc;
    }

    private String buildXmlValido() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <RESPONSE>
                    <status>OK</status>
                    <message>Success</message>
                    <product>
                        <producto>RECARGA TELCEL</producto>
                        <servicio>TELCEL</servicio>
                        <idServicio>10</idServicio>
                        <idProducto>1001</idProducto>
                        <idCatTipoServicio>1</idCatTipoServicio>
                        <tipoFront>1</tipoFront>
                        <hasDigitoVerificador>false</hasDigitoVerificador>
                        <precio>100.00</precio>
                        <showAyuda>false</showAyuda>
                    </product>
                </RESPONSE>
                """;
    }

    private FeignException buildFeignException(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "https://gestopago.portalventas.net/sistema/service/getProductList.do",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
        return FeignException.errorStatus("getProductList",
                feign.Response.builder()
                        .status(status)
                        .request(request)
                        .headers(Collections.emptyMap())
                        .build());
    }
}
