package com.proyecto.servicios.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload JSON que se cifra con AES-256-CBC y se envía a sendTx.do en el parámetro "signed".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GestoPagoSendTxPayload {

    private Integer idProducto;
    private Integer idServicio;
    private String telefono;
    private String horaLocal;
    private String upc;
    private String unidad;
    private String referencia;
    private BigDecimal montoPago;
}
