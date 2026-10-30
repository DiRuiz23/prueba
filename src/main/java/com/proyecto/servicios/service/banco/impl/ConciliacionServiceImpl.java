package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.client.model.GestoPagoSendTxPayload;
import com.proyecto.servicios.client.model.GestoPagoTxResponseXml;
import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.enums.EstatusTransaccionServicio;
import com.proyecto.servicios.repository.banco.TransaccionServicioRepository;
import com.proyecto.servicios.service.banco.ConciliacionService;
import com.proyecto.servicios.service.banco.CuentaDebitoService;
import com.proyecto.servicios.service.gestopago.GestoPagoTxGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConciliacionServiceImpl implements ConciliacionService {

    private final TransaccionServicioRepository transaccionRepository;
    private final GestoPagoTxGateway gestoPagoTxGateway;
    private final CuentaDebitoService cuentaDebitoService;

    @Value("${gestopago.tx.unidad:083}")
    private String unidad;

    @Override
    @Scheduled(fixedDelayString = "${gestopago.conciliacion.intervalo-ms:30000}", initialDelay = 15000)
    public int procesarTransaccionesEnConciliacion() {
        OffsetDateTime ahora = OffsetDateTime.now();
        List<TransaccionServicio> candidatas = cuentaDebitoService
                .reclamarCandidatasConciliacion(ahora, 20);

        if (candidatas.isEmpty()) {
            return 0;
        }

        log.info("Iniciando ciclo de conciliación con {} transacciones pendientes.", candidatas.size());
        int procesadas = 0;

        for (TransaccionServicio tx : candidatas) {
            try {
                conciliarTransaccionIndividual(tx, ahora);
                procesadas++;
            } catch (Exception e) {
                log.error("Error al conciliar transacción individual. txId={}, upc={}",
                        tx.getId(), tx.getUpc(), e);
            }
        }
        return procesadas;
    }

    private void conciliarTransaccionIndividual(TransaccionServicio tx, OffsetDateTime ahora) {
        // 1. Verificar si ha superado el límite de 24 horas permitido por GestoPago
        if (tx.getFechaLimiteConciliacion() != null && ahora.isAfter(tx.getFechaLimiteConciliacion())) {
            log.warn("Transacción superó la ventana de 24 horas de confirmTx. Marcando REVISION_MANUAL. txId={}, upc={}",
                    tx.getId(), tx.getUpc());
            tx.setEstatus(EstatusTransaccionServicio.REVISION_MANUAL);
            tx.setMensajeRespuesta("Superado límite de 24 horas de conciliación con el proveedor. Requiere revisión manual.");
            transaccionRepository.save(tx);
            return;
        }

        // 2. Reconstruir el payload con los MISMOS parámetros exactos originales
        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idProducto(tx.getIdProducto())
                .idServicio(tx.getIdServicio())
                .horaLocal(tx.getHoraLocal())
                .upc(tx.getUpc())
                .unidad(unidad)
                .build();

        if (tx.getTipoFront() != null && tx.getTipoFront() == 1) {
            payload.setTelefono(tx.getTelefonoReferencia());
        } else {
            payload.setReferencia(tx.getTelefonoReferencia());
            payload.setMontoPago(tx.getMonto());
        }

        GestoPagoTxResponseXml confirmXml;
        try {
            confirmXml = gestoPagoTxGateway.consultarTransaccion(payload, tx.getUpc(), tx.getId());
        } catch (Exception e) {
            log.warn("Fallo temporal al consultar confirmTx para txId={}. Se reprogramará.", tx.getId());
            reprogramarSiguienteIntento(tx, ahora);
            return;
        }

        String codigo = confirmXml != null ? confirmXml.getCodigo() : null;
        log.info("Resultado de confirmTx para txId={}, upc={}: código={}", tx.getId(), tx.getUpc(), codigo);

        if ("06".equals(codigo)) {
            // Según la documentación oficial: '06' en confirmTx significa que la transacción fue APLICADA
            log.info("confirmTx retornó 06: La transacción fue aplicada exitosamente. txId={}", tx.getId());
            cuentaDebitoService.confirmarExito(tx, confirmXml);
        } else if ("70".equals(codigo)) {
            // Según la documentación oficial: '70' en confirmTx significa que la transacción NO fue aplicada
            log.info("confirmTx retornó 70: La transacción no fue aplicada. Se reembolsa el dinero. txId={}", tx.getId());
            cuentaDebitoService.ejecutarReembolsoConciliacion(tx, "Conciliación determinó transacción no aplicada (código 70)");
        } else {
            // Cualquier otro código (82 o temporal) mantiene la transacción en conciliación
            reprogramarSiguienteIntento(tx, ahora);
        }
    }

    private void reprogramarSiguienteIntento(TransaccionServicio tx, OffsetDateTime ahora) {
        int intentos = tx.getIntentosConfirmacion() + 1;
        tx.setIntentosConfirmacion(intentos);

        // Backoff progresivo: 1 min, 2 min, 4 min, 8 min, ..., tope de 1 hora
        long delaySegundos = Math.min(3600L, 60L * (1L << Math.min(intentos, 6)));
        tx.setProximaConfirmacionAt(ahora.plusSeconds(delaySegundos));
        transaccionRepository.save(tx);
        log.info("Transacción reprogramada para próximo intento de confirmTx en {} segundos. txId={}",
                delaySegundos, tx.getId());
    }
}

