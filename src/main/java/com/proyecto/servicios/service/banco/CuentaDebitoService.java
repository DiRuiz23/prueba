package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.client.model.GestoPagoTxResponseXml;
import com.proyecto.servicios.dto.request.banco.PagoServicioRequest;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.service.banco.strategy.ValidacionMontoResult;

/**
 * Servicio encargado de las transacciones cortas de base de datos para la reserva,
 * confirmación y reembolso seguro de fondos, junto con la bitácora del libro contable (ledger).
 */
public interface CuentaDebitoService {

    /**
     * Transacción corta 1: Débito atómico condicional en saldo, inserción de transacción PENDIENTE
     * e inserción del registro inmutable en el ledger de movimientos.
     */
    TransaccionServicio reservarFondosYRegistrar(
            Cuenta cuenta,
            Usuario usuario,
            PagoServicioRequest request,
            ValidacionMontoResult montos,
            String idempotencyKey,
            String requestHash,
            String upc,
            String horaLocal,
            String payloadHash
    );

    /**
     * Transacción corta 2 (éxito): Actualiza la transacción a estado EXITOSA con los datos del proveedor.
     */
    void confirmarExito(TransaccionServicio tx, GestoPagoTxResponseXml txXml);

    /**
     * Transacción corta 2 (falla definitiva): Reembolsa los fondos atómicamente, registra el reverso
     * en el ledger y actualiza la transacción a FALLIDA.
     */
    void registrarFalloYReembolsar(TransaccionServicio tx, GestoPagoTxResponseXml txXml, String motivo);

    /**
     * Transacción corta 2 (resultado incierto): Actualiza la transacción a estado EN_CONCILIACION
     * programando el primer intento de confirmTx en no menos de 62 segundos. El saldo permanece retenido.
     */
    void registrarEnConciliacion(TransaccionServicio tx, String codigoRespuesta, String motivo);

    /**
     * Reembolso ejecutado por el worker de conciliación cuando confirmTx determina que la operación no fue aplicada.
     */
    void ejecutarReembolsoConciliacion(TransaccionServicio tx, String motivo);

    /**
     * Reclama en una transacción corta (FOR UPDATE SKIP LOCKED) un lote de transacciones candidatas
     * a conciliación y reserva temporalmente su ventana de ejecución para evitar colisiones entre nodos,
     * liberando la conexión de base de datos antes de realizar la llamada HTTP confirmTx.
     */
    java.util.List<TransaccionServicio> reclamarCandidatasConciliacion(java.time.OffsetDateTime ahora, int limite);
}

