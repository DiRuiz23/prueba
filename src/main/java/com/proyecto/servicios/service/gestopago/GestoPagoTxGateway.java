package com.proyecto.servicios.service.gestopago;

import com.proyecto.servicios.client.model.GestoPagoSendTxPayload;
import com.proyecto.servicios.client.model.GestoPagoTxResponseXml;

/**
 * Puerto de dominio para la comunicación transaccional con la pasarela de GestoPago.
 * Desacopla la lógica de negocio de los detalles de red, cifrado AES, cabeceras HTTP y JAXB.
 */
public interface GestoPagoTxGateway {

    /**
     * Envía una transacción de pago o recarga a través de sendTx.do.
     *
     * @param payload       datos canónicos de la operación a cifrar
     * @param upc           código único de transacción
     * @param transaccionId identificador de la transacción para auditoría
     * @return respuesta parseada de GestoPago
     */
    GestoPagoTxResponseXml enviarTransaccion(GestoPagoSendTxPayload payload, String upc, Long transaccionId);

    /**
     * Consulta el estado final de una transacción en estado incierto mediante confirmTx.do.
     * Utiliza idénticos parámetros de cifrado que la transacción original.
     *
     * @param payload       mismo payload exacto enviado en la transacción original
     * @param upc           mismo UPC
     * @param transaccionId identificador de la transacción
     * @return respuesta parseada de confirmación
     */
    GestoPagoTxResponseXml consultarTransaccion(GestoPagoSendTxPayload payload, String upc, Long transaccionId);
}

