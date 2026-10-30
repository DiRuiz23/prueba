package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.client.model.GestoPagoTxResponseXml;
import com.proyecto.servicios.dto.request.banco.PagoServicioRequest;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.MovimientoCuenta;
import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.enums.EstatusTransaccionServicio;
import com.proyecto.servicios.enums.TipoMovimientoCuenta;
import com.proyecto.servicios.exception.banco.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.banco.SaldoInsuficienteException;
import com.proyecto.servicios.repository.banco.CuentaRepository;
import com.proyecto.servicios.repository.banco.MovimientoCuentaRepository;
import com.proyecto.servicios.repository.banco.TransaccionServicioRepository;
import com.proyecto.servicios.service.banco.CuentaDebitoService;
import com.proyecto.servicios.service.banco.strategy.ValidacionMontoResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CuentaDebitoServiceImpl implements CuentaDebitoService {

    private final CuentaRepository cuentaRepository;
    private final TransaccionServicioRepository transaccionRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionServicio reservarFondosYRegistrar(
            Cuenta cuenta,
            Usuario usuario,
            PagoServicioRequest request,
            ValidacionMontoResult montos,
            String idempotencyKey,
            String requestHash,
            String upc,
            String horaLocal,
            String payloadHash
    ) {
        BigDecimal totalDebito = montos.montoDebitadoTotal();

        // 1. Débito atómico condicional
        int filasActualizadas = cuentaRepository.debitarSaldoAtomico(cuenta.getId(), totalDebito);
        if (filasActualizadas == 0) {
            log.warn("Débito rechazado por fondos insuficientes o cuenta inactiva. cuentaId={}, totalRequerido={}",
                    cuenta.getId(), totalDebito);
            throw new SaldoInsuficienteException(String.format(
                    "Fondos insuficientes en la cuenta para cubrir la operación (Monto total con comisión: $%s).",
                    totalDebito.toPlainString()
            ));
        }

        // 2. Obtener saldos exactos tras la actualización atómica
        Cuenta cuentaActualizada = cuentaRepository.findById(cuenta.getId())
                .orElseThrow(() -> new CuentaNoEncontradaException("La cuenta no fue encontrada."));
        BigDecimal saldoPosterior = cuentaActualizada.getSaldo();
        BigDecimal saldoAnterior = saldoPosterior.add(totalDebito);

        // 3. Crear registro de transacción en estado PENDIENTE
        TransaccionServicio tx = TransaccionServicio.builder()
                .cuenta(cuentaActualizada)
                .usuario(usuario)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .horaLocal(horaLocal)
                .payloadHash(payloadHash)
                .upc(upc)
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .tipoFront(montos.tipoFront() != null ? montos.tipoFront() : 1)
                .telefonoReferencia(request.getTelefonoReferencia())
                .monto(montos.montoOperacion())
                .comision(montos.comision())
                .montoDebitado(totalDebito)
                .saldoAnterior(saldoAnterior)
                .saldoPosterior(saldoPosterior)
                .estatus(EstatusTransaccionServicio.PENDIENTE)
                .build();
        tx = transaccionRepository.save(tx);

        // 4. Registrar movimiento en ledger contable inmutable
        MovimientoCuenta movimiento = MovimientoCuenta.builder()
                .cuenta(cuentaActualizada)
                .transaccionServicio(tx)
                .tipo(TipoMovimientoCuenta.DEBITO_PAGO)
                .monto(totalDebito)
                .saldoAnterior(saldoAnterior)
                .saldoPosterior(saldoPosterior)
                .build();
        movimientoCuentaRepository.save(movimiento);

        log.info("Fondos reservados exitosamente. txId={}, upc={}, montoDebitado={}, nuevoSaldo={}",
                tx.getId(), upc, totalDebito, saldoPosterior);
        return tx;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmarExito(TransaccionServicio tx, GestoPagoTxResponseXml txXml) {
        TransaccionServicio txActual = transaccionRepository.findById(tx.getId())
                .orElseThrow(() -> new IllegalStateException("Transacción no encontrada: " + tx.getId()));

        txActual.setEstatus(EstatusTransaccionServicio.EXITOSA);
        if (txXml != null) {
            txActual.setIdTxGestopago(txXml.getIdTx());
            txActual.setNumeroAutorizacion(txXml.getNumAutorizacion());
            txActual.setCodigoRespuesta(txXml.getCodigo());
            txActual.setMensajeRespuesta(txXml.getTexto() != null ? txXml.getTexto() : "Transacción exitosa");
        }
        transaccionRepository.save(txActual);
        log.info("Transacción confirmada EXITOSA. txId={}, upc={}, folioGestoPago={}",
                txActual.getId(), txActual.getUpc(), txActual.getIdTxGestopago());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalloYReembolsar(TransaccionServicio tx, GestoPagoTxResponseXml txXml, String motivo) {
        TransaccionServicio txActual = transaccionRepository.findById(tx.getId())
                .orElseThrow(() -> new IllegalStateException("Transacción no encontrada: " + tx.getId()));

        if (txActual.getEstatus() == EstatusTransaccionServicio.PENDIENTE) {
            // Reembolsar saldo atómicamente
            cuentaRepository.reembolsarSaldoAtomico(txActual.getCuenta().getId(), txActual.getMontoDebitado());

            Cuenta cuentaActualizada = cuentaRepository.findById(txActual.getCuenta().getId())
                    .orElseThrow(() -> new CuentaNoEncontradaException("La cuenta no fue encontrada"));
            BigDecimal saldoPosterior = cuentaActualizada.getSaldo();
            BigDecimal saldoAnterior = saldoPosterior.subtract(txActual.getMontoDebitado());

            // Registrar movimiento de REVERSO en el ledger
            MovimientoCuenta movimiento = MovimientoCuenta.builder()
                    .cuenta(cuentaActualizada)
                    .transaccionServicio(txActual)
                    .tipo(TipoMovimientoCuenta.REVERSO_PAGO)
                    .monto(txActual.getMontoDebitado())
                    .saldoAnterior(saldoAnterior)
                    .saldoPosterior(saldoPosterior)
                    .build();
            movimientoCuentaRepository.save(movimiento);

            // Actualizar transacción a FALLIDA
            txActual.setEstatus(EstatusTransaccionServicio.FALLIDA);
            if (txXml != null) {
                txActual.setIdTxGestopago(txXml.getIdTx());
                txActual.setNumeroAutorizacion(txXml.getNumAutorizacion());
                txActual.setCodigoRespuesta(txXml.getCodigo());
                txActual.setMensajeRespuesta(txXml.getTexto() != null ? txXml.getTexto() : motivo);
            } else {
                txActual.setMensajeRespuesta(motivo);
            }
            transaccionRepository.save(txActual);
            log.info("Transacción FALLIDA y saldo reembolsado. txId={}, upc={}, saldoRestaurado={}",
                    txActual.getId(), txActual.getUpc(), saldoPosterior);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEnConciliacion(TransaccionServicio tx, String codigoRespuesta, String motivo) {
        TransaccionServicio txActual = transaccionRepository.findById(tx.getId())
                .orElseThrow(() -> new IllegalStateException("Transacción no encontrada: " + tx.getId()));

        if (txActual.getEstatus() == EstatusTransaccionServicio.PENDIENTE) {
            txActual.setEstatus(EstatusTransaccionServicio.EN_CONCILIACION);
            // El proveedor exige esperar al menos 62 segundos antes de la primera consulta confirmTx
            txActual.setProximaConfirmacionAt(OffsetDateTime.now().plusSeconds(62));
            txActual.setFechaLimiteConciliacion(OffsetDateTime.now().plusHours(24));
            txActual.setCodigoRespuesta(codigoRespuesta);
            txActual.setMensajeRespuesta(motivo);
            transaccionRepository.save(txActual);
            log.warn("Transacción en estado incierto enviada a EN_CONCILIACION. txId={}, upc={}, motivo={}",
                    txActual.getId(), txActual.getUpc(), motivo);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ejecutarReembolsoConciliacion(TransaccionServicio tx, String motivo) {
        TransaccionServicio txActual = transaccionRepository.findById(tx.getId())
                .orElseThrow(() -> new IllegalStateException("Transacción no encontrada: " + tx.getId()));

        if (txActual.getEstatus() == EstatusTransaccionServicio.EN_CONCILIACION) {
            cuentaRepository.reembolsarSaldoAtomico(txActual.getCuenta().getId(), txActual.getMontoDebitado());

            Cuenta cuentaActualizada = cuentaRepository.findById(txActual.getCuenta().getId())
                    .orElseThrow(() -> new CuentaNoEncontradaException("La cuenta no fue encontrada"));
            BigDecimal saldoPosterior = cuentaActualizada.getSaldo();
            BigDecimal saldoAnterior = saldoPosterior.subtract(txActual.getMontoDebitado());

            MovimientoCuenta movimiento = MovimientoCuenta.builder()
                    .cuenta(cuentaActualizada)
                    .transaccionServicio(txActual)
                    .tipo(TipoMovimientoCuenta.REVERSO_PAGO)
                    .monto(txActual.getMontoDebitado())
                    .saldoAnterior(saldoAnterior)
                    .saldoPosterior(saldoPosterior)
                    .build();
            movimientoCuentaRepository.save(movimiento);

            txActual.setEstatus(EstatusTransaccionServicio.REVERSADA);
            txActual.setMensajeRespuesta(motivo);
            transaccionRepository.save(txActual);
            log.info("Conciliación determinó transacción no aplicada. Saldo reembolsado y marcada REVERSADA. txId={}",
                    txActual.getId());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public java.util.List<TransaccionServicio> reclamarCandidatasConciliacion(OffsetDateTime ahora, int limite) {
        java.util.List<TransaccionServicio> candidatas = transaccionRepository
                .findCandidatasConciliacionParaProcesar(ahora, limite);
        for (TransaccionServicio tx : candidatas) {
            // Reserva temporal (lease de 90s) bajo el bloqueo FOR UPDATE SKIP LOCKED
            // antes de liberar la conexión de BD e invocar confirmTx por HTTP
            tx.setProximaConfirmacionAt(ahora.plusSeconds(90));
        }
        if (!candidatas.isEmpty()) {
            transaccionRepository.saveAll(candidatas);
        }
        return candidatas;
    }
}

