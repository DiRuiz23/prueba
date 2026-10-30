package com.proyecto.servicios.service.banco;

public interface ConciliacionService {

    /**
     * Procesa un lote de transacciones en estado EN_CONCILIACION con proxima_confirmacion_at cumplida.
     * Consulta confirmTx.do para determinar si la operación fue aplicada (06) o rechazada (70).
     *
     * @return número de transacciones conciliadas en el ciclo
     */
    int procesarTransaccionesEnConciliacion();
}

