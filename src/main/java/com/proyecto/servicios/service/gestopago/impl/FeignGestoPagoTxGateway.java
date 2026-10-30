package com.proyecto.servicios.service.gestopago.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.client.model.GestoPagoSendTxPayload;
import com.proyecto.servicios.client.model.GestoPagoTxResponseXml;
import com.proyecto.servicios.entity.banco.GestoPagoBitacora;
import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.enums.TipoOperacionBitacora;
import com.proyecto.servicios.exception.gestopago.ProveedorNoDisponibleException;
import com.proyecto.servicios.exception.gestopago.ResultadoInciertoException;
import com.proyecto.servicios.repository.banco.GestoPagoBitacoraRepository;
import com.proyecto.servicios.service.gestopago.GestoPagoTokenService;
import com.proyecto.servicios.service.gestopago.GestoPagoTxGateway;
import com.proyecto.servicios.util.AesCifradoUtil;
import feign.FeignException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;

@Service
@Slf4j
public class FeignGestoPagoTxGateway implements GestoPagoTxGateway {

    private final GestoPagoTxClient gestoPagoTxClient;
    private final GestoPagoTokenService tokenService;
    private final AesCifradoUtil aesCifradoUtil;
    private final ObjectMapper objectMapper;
    private final GestoPagoBitacoraRepository bitacoraRepository;

    @Value("${gestopago.tx.x-api-key:}")
    private String apiKey;

    public FeignGestoPagoTxGateway(
            GestoPagoTxClient gestoPagoTxClient,
            GestoPagoTokenService tokenService,
            AesCifradoUtil aesCifradoUtil,
            ObjectMapper objectMapper,
            GestoPagoBitacoraRepository bitacoraRepository
    ) {
        this.gestoPagoTxClient = gestoPagoTxClient;
        this.tokenService = tokenService;
        this.aesCifradoUtil = aesCifradoUtil;
        this.objectMapper = objectMapper;
        this.bitacoraRepository = bitacoraRepository;
    }

    @Override
    public GestoPagoTxResponseXml enviarTransaccion(GestoPagoSendTxPayload payload, String upc, Long transaccionId) {
        return ejecutarLlamadaConResiliencia(payload, upc, transaccionId, TipoOperacionBitacora.SEND_TX);
    }

    @Override
    public GestoPagoTxResponseXml consultarTransaccion(GestoPagoSendTxPayload payload, String upc, Long transaccionId) {
        return ejecutarLlamadaConResiliencia(payload, upc, transaccionId, TipoOperacionBitacora.CONFIRM_TX);
    }

    private GestoPagoTxResponseXml ejecutarLlamadaConResiliencia(
            GestoPagoSendTxPayload payload,
            String upc,
            Long transaccionId,
            TipoOperacionBitacora tipoOperacion
    ) {
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al serializar payload de transacción a JSON", e);
        }

        String random = String.valueOf(System.currentTimeMillis());
        String signed = aesCifradoUtil.cifrar(jsonPayload);
        String requestEnmascarado = enmascararPayloadParaBitacora(jsonPayload);

        long inicioMs = System.currentTimeMillis();
        String responseXmlStr = null;
        int httpStatus = 200;
        boolean tokenRefrescado = false;

        String token = tokenService.obtenerTokenBearerValido();

        try {
            responseXmlStr = llamarEndpoint(tipoOperacion, token, random, signed);
        } catch (FeignException.Forbidden | FeignException.Unauthorized e) {
            // REGLA: Refresh-once ante token expirado (403 EXPIRED)
            log.warn("Token rechazado por GestoPago ({}). Ejecutando refresh-once. txId={}",
                    e.status(), transaccionId);
            try {
                token = tokenService.forzarRenovacionYObtenerToken();
                tokenRefrescado = true;
                responseXmlStr = llamarEndpoint(tipoOperacion, token, random, signed);
            } catch (Exception reintentoEx) {
                log.error("Fallo definitivo tras refresh-once de token. txId={}", transaccionId, reintentoEx);
                registrarBitacora(transaccionId, upc, tipoOperacion, requestEnmascarado,
                        "ERROR_AUTH: " + reintentoEx.getMessage(), 403, "AUTH_FAIL",
                        System.currentTimeMillis() - inicioMs);
                throw new ProveedorNoDisponibleException("No fue posible autenticar la transacción con el proveedor.", reintentoEx);
            }
        } catch (FeignException e) {
            long duracion = System.currentTimeMillis() - inicioMs;
            httpStatus = e.status() > 0 ? e.status() : 504;
            log.error("Error Feign al comunicar con GestoPago. status={}, txId={}, upc={}",
                    httpStatus, transaccionId, upc);

            registrarBitacora(transaccionId, upc, tipoOperacion, requestEnmascarado,
                    "FEIGN_ERROR: " + e.getMessage(), httpStatus, "ERROR", duracion);

            // Timeout o error 5xx del proveedor: RESULTADO INCIERTO
            throw new ResultadoInciertoException(
                    "Tiempo de espera agotado o error de comunicación con GestoPago. La transacción queda en conciliación.",
                    null
            );
        } catch (Exception e) {
            long duracion = System.currentTimeMillis() - inicioMs;
            log.error("Excepción inesperada de red/comunicación con GestoPago. txId={}, upc={}",
                    transaccionId, upc, e);

            registrarBitacora(transaccionId, upc, tipoOperacion, requestEnmascarado,
                    "NETWORK_ERROR: " + e.getMessage(), 504, "ERROR", duracion);

            throw new ResultadoInciertoException(
                    "Fallo de conexión con la pasarela de pagos. La transacción queda en proceso de conciliación.",
                    null
            );
        }

        long duracionMs = System.currentTimeMillis() - inicioMs;

        // 3. Parsear respuesta XML
        GestoPagoTxResponseXml responseXml;
        try {
            responseXml = parsearXml(responseXmlStr);
        } catch (Exception e) {
            log.error("Respuesta XML malformada o vacía recibida de GestoPago. txId={}, raw={}",
                    transaccionId, responseXmlStr);

            registrarBitacora(transaccionId, upc, tipoOperacion, requestEnmascarado,
                    "XML_PARSE_ERROR: " + responseXmlStr, httpStatus, "PARSE_ERR", duracionMs);

            throw new ResultadoInciertoException(
                    "El proveedor retornó una respuesta ilegible o malformada. La transacción queda en conciliación.",
                    null
            );
        }

        String codigoRespuesta = responseXml.getCodigo();
        String enmascaradoResponse = enmascararXmlParaBitacora(responseXmlStr);

        // 4. Guardar bitácora técnica oficial exigida por GestoPago
        registrarBitacora(transaccionId, upc, tipoOperacion, requestEnmascarado,
                enmascaradoResponse, httpStatus, codigoRespuesta, duracionMs);

        // 5. Analizar códigos de resultado incierto del proveedor
        if (tipoOperacion == TipoOperacionBitacora.SEND_TX) {
            if ("82".equals(codigoRespuesta)) {
                log.warn("GestoPago reportó código 82 (timeout interno de la pasarela). txId={}", transaccionId);
                throw new ResultadoInciertoException(
                        "El proveedor reportó código 82 (tiempo de espera agotado internamente). Se conciliará con confirmTx.",
                        null
                );
            }
            if ("06".equals(codigoRespuesta)) {
                log.warn("GestoPago reportó código 06 en sendTx (posible duplicidad/en proceso). txId={}", transaccionId);
                throw new ResultadoInciertoException(
                        "El proveedor reportó código 06 (posible operación previa en proceso). Se conciliará el estado con confirmTx.",
                        null
                );
            }
        }

        return responseXml;
    }

    private String llamarEndpoint(TipoOperacionBitacora tipo, String token, String random, String signed) {
        String authHeader = "Bearer " + token;
        if (tipo == TipoOperacionBitacora.CONFIRM_TX) {
            return gestoPagoTxClient.confirmTx(authHeader, apiKey, random, signed);
        } else {
            return gestoPagoTxClient.sendTx(authHeader, apiKey, random, signed);
        }
    }

    private GestoPagoTxResponseXml parsearXml(String xml) throws Exception {
        if (xml == null || xml.trim().isEmpty()) {
            throw new IllegalArgumentException("La respuesta XML está vacía");
        }
        JAXBContext context = JAXBContext.newInstance(GestoPagoTxResponseXml.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (GestoPagoTxResponseXml) unmarshaller.unmarshal(new StringReader(xml.trim()));
    }

    private void registrarBitacora(
            Long transaccionId,
            String upc,
            TipoOperacionBitacora tipoOperacion,
            String requestEnmascarado,
            String responseEnmascarado,
            int codigoHttp,
            String codigoRespuestaProveedor,
            long duracionMs
    ) {
        try {
            TransaccionServicio txRef = transaccionId != null ? TransaccionServicio.builder().id(transaccionId).build() : null;
            GestoPagoBitacora bitacora = GestoPagoBitacora.builder()
                    .transaccionServicio(txRef)
                    .upc(upc)
                    .tipoOperacion(tipoOperacion)
                    .requestEnmascarado(requestEnmascarado)
                    .responseEnmascarado(responseEnmascarado)
                    .codigoHttp(codigoHttp)
                    .codigoRespuestaProveedor(codigoRespuestaProveedor)
                    .duracionMs(duracionMs)
                    .build();
            bitacoraRepository.save(bitacora);
        } catch (Exception e) {
            log.warn("No fue posible guardar el registro de bitácora técnica: {}", e.getMessage());
        }
    }

    private String enmascararPayloadParaBitacora(String json) {
        if (json == null) return null;
        // Enmascarar números de teléfono y referencias (dejar solo los últimos 4 dígitos)
        return json.replaceAll("(\"telefono\"\\s*:\\s*\")(\\d{0,16})(\\d{4}\")", "$1******$3")
                   .replaceAll("(\"referencia\"\\s*:\\s*\")(.{0,96})(.{4}\")", "$1******$3");
    }

    private String enmascararXmlParaBitacora(String xml) {
        if (xml == null) return null;
        return xml.replaceAll("(<REFERENCIA>)(.{0,96})(.{4}</REFERENCIA>)", "$1******$3");
    }
}

