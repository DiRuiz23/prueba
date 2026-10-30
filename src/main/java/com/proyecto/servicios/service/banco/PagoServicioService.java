package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.dto.request.banco.PagoServicioRequest;
import com.proyecto.servicios.dto.response.banco.PagoServicioResponse;

public interface PagoServicioService {

    /**
     * Procesa una transacción de pago de servicios o recarga electrónica:
     * 1. Valida titularidad de la cuenta y PIN transaccional (con bloqueo temporal).
     * 2. Valida catálogo, tipoFront, límites y calcula montos y comisiones por estrategia.
     * 3. Valida idempotencia (evita dobles cargos).
     * 4. Reserva fondos en transacción corta previa (débito atómico condicional + ledger).
     * 5. Envía la transacción a GestoPago sendTx (fuera de transacción de base de datos).
     * 6. Actualiza el estado (EXITOSA si '01', FALLIDA + reembolso si error definitivo, o EN_CONCILIACION si timeout/incierto).
     *
     * @param request           datos de la operación
     * @param correoAutenticado identidad tomada del JWT
     * @param idempotencyKey    clave de idempotencia (cabecera HTTP)
     * @return comprobante de la transacción
     */
    PagoServicioResponse realizarPagoServicio(PagoServicioRequest request, String correoAutenticado, String idempotencyKey);

    /**
     * Consulta el estado actual de una transacción por su ID, verificando que pertenezca al usuario autenticado.
     */
    PagoServicioResponse consultarEstadoTransaccion(Long idTransaccion, String correoAutenticado);
}
