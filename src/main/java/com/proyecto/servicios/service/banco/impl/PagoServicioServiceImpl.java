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
import com.proyecto.servicios.exception.banco.SaldoInsuficienteException;
import com.proyecto.servicios.exception.gestopago.ProveedorException;
import com.proyecto.servicios.exception.gestopago.ResultadoInciertoException;
import com.proyecto.servicios.repository.banco.TransaccionServicioRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.service.banco.AutorizacionOperacionService;
import com.proyecto.servicios.service.banco.CuentaDebitoService;
import com.proyecto.servicios.service.banco.NotificacionTransaccionService;
import com.proyecto.servicios.service.banco.NotificacionTransaccionService.NotificacionTransaccionData;
import com.proyecto.servicios.service.banco.PagoServicioService;
import com.proyecto.servicios.service.banco.strategy.ValidacionMontoResult;
import com.proyecto.servicios.service.gestopago.CatalogoPagoValidator;
import com.proyecto.servicios.service.gestopago.GestoPagoTxGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoServicioServiceImpl implements PagoServicioService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AutorizacionOperacionService autorizacionOperacionService;
    private final CatalogoPagoValidator catalogoPagoValidator;
    private final CuentaDebitoService cuentaDebitoService;
    private final GestoPagoTxGateway gestoPagoTxGateway;
    private final TransaccionServicioRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    @Value("${gestopago.tx.unidad:083}")
    private String unidad;

    @Autowired(required = false)
    private NotificacionTransaccionService notificacionTransaccionService;

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
        String horaLocal = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now(ZoneId.systemDefault()));
        String upc = generarUpcUnico();

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
        String nombreServicio = resolverNombreServicio(producto);

        // 5. PASO 1 (TX CORTA PREVIA): Debitar saldo atómicamente e insertar transacción PENDIENTE y ledger
        TransaccionServicio tx;
        try {
            tx = cuentaDebitoService.reservarFondosYRegistrar(
                    cuenta, usuario, request, montos, idempotencyKey, requestHash, upc, horaLocal, payloadHash
            );
        } catch (SaldoInsuficienteException e) {
            notificarResultado(
                    correoAutenticado,
                    false,
                    nombreServicio,
                    null,
                    cuenta.getNumeroCuenta(),
                    request.getTelefonoReferencia(),
                    montos.montoDebitadoTotal(),
                    cuenta.getSaldo(),
                    null,
                    null,
                    e.getMessage(),
                    OffsetDateTime.now(ZoneId.systemDefault())
            );
            throw e;
        }

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

            String mensajeExito = txXml.getTexto() != null ? txXml.getTexto() : "Operación realizada con éxito";
            OffsetDateTime fechaExito = OffsetDateTime.now(ZoneId.systemDefault());
            notificarResultado(
                    correoAutenticado,
                    true,
                    nombreServicio,
                    upc,
                    cuenta.getNumeroCuenta(),
                    request.getTelefonoReferencia(),
                    montos.montoDebitadoTotal(),
                    tx.getSaldoPosterior(),
                    txXml.getNumAutorizacion(),
                    txXml.getIdTx(),
                    mensajeExito,
                    fechaExito
            );

            return PagoServicioResponse.builder()
                    .idTransaccion(tx.getId())
                    .upc(upc)
                    .idTxGestoPago(txXml.getIdTx())
                    .numeroAutorizacion(txXml.getNumAutorizacion())
                    .numeroCuenta(cuenta.getNumeroCuenta())
                    .montoDebitado(montos.montoDebitadoTotal())
                    .saldoRestante(tx.getSaldoPosterior())
                    .telefonoReferencia(request.getTelefonoReferencia())
                    .idProducto(request.getIdProducto())
                    .idServicio(request.getIdServicio())
                    .estatus("EXITOSA")
                    .mensaje(mensajeExito)
                    .fechaTransaccion(fechaExito)
                    .build();
        } else {
            String textoCrudo = txXml != null ? txXml.getTexto() : null;
            String codigo = txXml != null ? txXml.getCodigo() : "ERR";
            String motivo = sanitizarMotivoProveedor(codigo, textoCrudo);

            log.warn("Rechazo definitivo del proveedor (código {}). Reembolsando saldo. txId={}, detalleProveedor={}",
                    codigo, tx.getId(), textoCrudo);
            cuentaDebitoService.registrarFalloYReembolsar(tx, txXml, motivo);

            notificarResultado(
                    correoAutenticado,
                    false,
                    nombreServicio,
                    upc,
                    cuenta.getNumeroCuenta(),
                    request.getTelefonoReferencia(),
                    montos.montoDebitadoTotal(),
                    tx.getSaldoAnterior() != null ? tx.getSaldoAnterior() : cuenta.getSaldo(),
                    null,
                    txXml != null ? txXml.getIdTx() : null,
                    motivo,
                    tx.getFechaTransaccion() != null ? tx.getFechaTransaccion() : OffsetDateTime.now(ZoneId.systemDefault())
            );

            throw new ProveedorException(construirMensajeExcepcionProveedor(motivo, codigo), codigo, tx);
        }
    }

    @Override
    public PagoServicioResponse consultarEstadoTransaccion(Long idTransaccion, String correoAutenticado) {
        TransaccionServicio tx = transaccionRepository.findById(idTransaccion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la transacción solicitada."));

        // Validar titularidad
        Usuario usuario = usuarioRepository.findByCorreo(correoAutenticado).orElse(null);
        if (usuario != null && tx.getCuenta() != null && tx.getCuenta().getCliente() != null
                && !tx.getCuenta().getCliente().getId().equals(usuario.getCliente().getId())) {
            throw new RecursoNoEncontradoException("No se encontró la transacción solicitada.");
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
                .idProducto(tx.getIdProducto())
                .idServicio(tx.getIdServicio())
                .estatus(estatus)
                .mensaje(tx.getMensajeRespuesta() != null ? tx.getMensajeRespuesta() : "Estado actual de la transacción")
                .fechaTransaccion(tx.getFechaTransaccion())
                .build();
    }

    private String resolverNombreServicio(ProductoItem producto) {
        if (producto == null) {
            return "Pago de servicio";
        }
        if (producto.getProducto() != null && !producto.getProducto().isBlank()) {
            return producto.getProducto().trim();
        }
        if (producto.getServicio() != null && !producto.getServicio().isBlank()) {
            return producto.getServicio().trim();
        }
        return "Pago de servicio";
    }

    private void notificarResultado(
            String correoDestino,
            boolean exitosa,
            String nombreServicio,
            String upc,
            String numeroCuenta,
            String telefonoReferencia,
            BigDecimal monto,
            BigDecimal saldoRestante,
            String numeroAutorizacion,
            String idTxGestoPago,
            String motivo,
            OffsetDateTime fechaTransaccion
    ) {
        if (notificacionTransaccionService == null) {
            return;
        }
        notificacionTransaccionService.notificarResultadoTransaccion(new NotificacionTransaccionData(
                correoDestino,
                exitosa,
                nombreServicio,
                upc,
                numeroCuenta,
                telefonoReferencia,
                monto,
                saldoRestante,
                numeroAutorizacion,
                idTxGestoPago,
                motivo,
                fechaTransaccion
        ));
    }

    private String construirMensajeExcepcionProveedor(String motivo, String codigo) {
        if ("Transacción rechazada por el proveedor".equalsIgnoreCase(motivo)) {
            return "Transacción rechazada por el proveedor (Código: " + codigo + ")";
        }
        return "Transacción rechazada por el proveedor: " + motivo + " (Código: " + codigo + ")";
    }

    private String sanitizarMotivoProveedor(String codigo, String textoCrudo) {
        if (textoCrudo == null || textoCrudo.isBlank()) {
            return "Transacción rechazada por el proveedor";
        }
        String limpio = textoCrudo.trim();
        if ("55".equals(codigo) || limpio.contains("validIPS=") || limpio.contains("validationCompleted=false")) {
            return "Transacción rechazada por el proveedor";
        }
        return limpio.length() > 240 ? limpio.substring(0, 240) + "..." : limpio;
    }

    private String generarUpcUnico() {
        String upc;
        int intentos = 0;
        do {
            int primeraParte = 10_000 + SECURE_RANDOM.nextInt(90_000);
            int segundaParte = SECURE_RANDOM.nextInt(100_000);
            upc = String.format("%05d%05d", primeraParte, segundaParte);
            intentos++;
        } while (transaccionRepository.existsByUpc(upc) && intentos < 10);
        return upc;
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
