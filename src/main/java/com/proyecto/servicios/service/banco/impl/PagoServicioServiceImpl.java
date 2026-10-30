package com.proyecto.servicios.service.banco.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.model.CatProductXml.ProductoItem;
import com.proyecto.servicios.client.model.GestoPagoSendTxPayload;
import com.proyecto.servicios.client.model.GestoPagoTxResponseXml;
import com.proyecto.servicios.dto.request.banco.PagoServicioRequest;
import com.proyecto.servicios.dto.response.banco.PagoServicioResponse;
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
import com.proyecto.servicios.service.banco.PagoServicioService;
import com.proyecto.servicios.service.banco.strategy.ValidacionMontoResult;
import com.proyecto.servicios.service.gestopago.CatalogoPagoValidator;
import com.proyecto.servicios.service.gestopago.GestoPagoTxGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoServicioServiceImpl implements PagoServicioService {

    private final AutorizacionOperacionService autorizacionOperacionService;
    private final CatalogoPagoValidator catalogoPagoValidator;
    private final CuentaDebitoService cuentaDebitoService;
    private final GestoPagoTxGateway gestoPagoTxGateway;
    private final TransaccionServicioRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    @Value("${gestopago.tx.unidad:083}")
    private String unidad;

    @Override
    public PagoServicioResponse realizarPagoServicio(PagoServicioRequest request, String correoAutenticado, String idempotencyKey) {
        log.info("Iniciando solicitud de pago de servicio. cuenta={}, producto={}, servicio={}",
                request.getNumeroCuenta(), request.getIdProducto(), request.getIdServicio());

        // 1. Validar identidad, titularidad de cuenta y PIN transaccional (con control de bloqueo)
        Cuenta cuenta = autorizacionOperacionService.obtenerCuentaAutorizada(request.getNumeroCuenta(), correoAutenticado);
        autorizacionOperacionService.validarPinTransaccional(cuenta, request.getPin());

        // 2. Validar idempotencia para evitar dobles cargos
        String requestHash = calcularSha256(
                request.getNumeroCuenta() + "|" + request.getIdProducto() + "|" +
                request.getIdServicio() + "|" + request.getTelefonoReferencia() + "|" +
                (request.getMonto() != null ? request.getMonto().toPlainString() : "0")
        );

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ReglaNegocioException("La cabecera 'Idempotency-Key' es obligatoria para procesar la operación.");
        }

        String claveIdempotenciaLimpia = idempotencyKey.trim();
        Optional<TransaccionServicio> existente = transaccionRepository
                .findByCuentaIdAndIdempotencyKey(cuenta.getId(), claveIdempotenciaLimpia);

        if (existente.isPresent()) {
            TransaccionServicio txExistente = existente.get();
            if (!requestHash.equals(txExistente.getRequestHash())) {
                throw new IdempotenciaConflictoException(
                        "La clave de idempotencia ya fue utilizada anteriormente con parámetros de operación distintos."
                );
            }
            if (txExistente.getEstatus() == EstatusTransaccionServicio.PENDIENTE) {
                throw new IdempotenciaConflictoException(
                        "La transacción con esta clave de idempotencia se encuentra actualmente en proceso. Intente consultar su estado en unos momentos."
                );
            }
            log.info("Operación idempotente detectada. Retornando resultado previo sin aplicar nuevo cargo. txId={}",
                    txExistente.getId());
            return construirRespuestaDesdeEntidad(txExistente);
        }

        // 3. Validar catálogo, vigencia, tipoFront y calcular montos por estrategia
        ValidacionMontoResult montos = catalogoPagoValidator.validarOperacion(
                request.getIdProducto(), request.getIdServicio(), request.getMonto(), request.getTelefonoReferencia()
        );
        ProductoItem producto = catalogoPagoValidator.obtenerYValidarProducto(
                request.getIdProducto(), request.getIdServicio()
        );

        // 4. Generar identificadores únicos y datos canónicos
        String horaLocal = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        // UPC: máximo 15 caracteres (ejemplo: GP + 12 dígitos)
        String upc = "GP" + (System.currentTimeMillis() % 100000000000L) + String.format("%02d", (int)(Math.random() * 100));
        if (upc.length() > 15) {
            upc = upc.substring(0, 15);
        }

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .horaLocal(horaLocal)
                .upc(upc)
                .unidad(unidad)
                .build();

        if (producto.getTipoFront() != null && producto.getTipoFront() == 1) {
            payload.setTelefono(request.getTelefonoReferencia());
        } else {
            payload.setReferencia(request.getTelefonoReferencia());
            payload.setMontoPago(montos.montoOperacion());
        }

        String payloadHash;
        try {
            payloadHash = calcularSha256(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            payloadHash = calcularSha256(upc + horaLocal);
        }

        Usuario usuario = usuarioRepository.findByCorreo(correoAutenticado).orElse(null);

        // 5. PASO 1 (TX CORTA PREVIA): Debitar saldo atómicamente e insertar transacción PENDIENTE y ledger
        TransaccionServicio tx = cuentaDebitoService.reservarFondosYRegistrar(
                cuenta, usuario, request, montos, idempotencyKey, requestHash, upc, horaLocal, payloadHash
        );

        // 6. PASO 2: Llamada HTTP al proveedor FUERA de cualquier transacción de base de datos
        GestoPagoTxResponseXml txXml;
        try {
            txXml = gestoPagoTxGateway.enviarTransaccion(payload, upc, tx.getId());
        } catch (ResultadoInciertoException e) {
            log.warn("Transacción en estado incierto tras sendTx. txId={}, upc={}", tx.getId(), upc);
            cuentaDebitoService.registrarEnConciliacion(tx, "82", e.getMessage());
            throw new ResultadoInciertoException(e.getMessage(), tx);
        } catch (Exception e) {
            log.error("Excepción al comunicar con proveedor. txId={}, upc={}", tx.getId(), upc, e);
            cuentaDebitoService.registrarEnConciliacion(tx, "ERROR", e.getMessage());
            throw new ResultadoInciertoException(
                    "Error de comunicación con GestoPago. La transacción quedó en conciliación para verificar con el proveedor.",
                    tx
            );
        }

        // 7. PASO 3 (TX CORTA POSTERIOR): Tratamiento del resultado
        if (txXml != null && txXml.esExitosa()) {
            cuentaDebitoService.confirmarExito(tx, txXml);
            log.info("Pago de servicio completado con éxito. txId={}, upc={}, folioGestoPago={}",
                    tx.getId(), upc, txXml.getIdTx());

            return PagoServicioResponse.builder()
                    .idTransaccion(tx.getId())
                    .upc(upc)
                    .idTxGestoPago(txXml.getIdTx())
                    .numeroAutorizacion(txXml.getNumAutorizacion())
                    .numeroCuenta(cuenta.getNumeroCuenta())
                    .montoDebitado(montos.montoDebitadoTotal())
                    .saldoRestante(tx.getSaldoPosterior())
                    .telefonoReferencia(request.getTelefonoReferencia())
                    .estatus("EXITOSA")
                    .mensaje(txXml.getTexto() != null ? txXml.getTexto() : "Operación realizada con éxito")
                    .fechaTransaccion(OffsetDateTime.now())
                    .build();
        } else {
            String motivo = txXml != null && txXml.getTexto() != null
                    ? txXml.getTexto()
                    : "Transacción rechazada por el proveedor";
            String codigo = txXml != null ? txXml.getCodigo() : "ERR";

            log.warn("Rechazo definitivo del proveedor (código {}). Reembolsando saldo. txId={}",
                    codigo, tx.getId());
            cuentaDebitoService.registrarFalloYReembolsar(tx, txXml, motivo);

            throw new ProveedorException("Transacción rechazada por el proveedor: " + motivo + " (Código: " + codigo + ")", codigo);
        }
    }

    @Override
    public PagoServicioResponse consultarEstadoTransaccion(Long idTransaccion, String correoAutenticado) {
        TransaccionServicio tx = transaccionRepository.findById(idTransaccion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la transacción solicitada."));

        // Validar titularidad
        Usuario usuario = usuarioRepository.findByCorreo(correoAutenticado).orElse(null);
        if (usuario != null && tx.getCuenta() != null && tx.getCuenta().getCliente() != null) {
            if (!tx.getCuenta().getCliente().getId().equals(usuario.getCliente().getId())) {
                throw new RecursoNoEncontradoException("No se encontró la transacción solicitada.");
            }
        }
        return construirRespuestaDesdeEntidad(tx);
    }

    private PagoServicioResponse construirRespuestaDesdeEntidad(TransaccionServicio tx) {
        String estatus = tx.getEstatus() == EstatusTransaccionServicio.EN_CONCILIACION
                ? "EN_PROCESO"
                : tx.getEstatus().name();

        return PagoServicioResponse.builder()
                .idTransaccion(tx.getId())
                .upc(tx.getUpc())
                .idTxGestoPago(tx.getIdTxGestopago())
                .numeroAutorizacion(tx.getNumeroAutorizacion())
                .numeroCuenta(tx.getCuenta() != null ? tx.getCuenta().getNumeroCuenta() : null)
                .montoDebitado(tx.getMontoDebitado())
                .saldoRestante(tx.getSaldoPosterior())
                .telefonoReferencia(tx.getTelefonoReferencia())
                .estatus(estatus)
                .mensaje(tx.getMensajeRespuesta() != null ? tx.getMensajeRespuesta() : "Estado actual de la transacción")
                .fechaTransaccion(tx.getFechaTransaccion())
                .build();
    }

    private String calcularSha256(String entrada) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(entrada.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en el sistema", e);
        }
    }
}
