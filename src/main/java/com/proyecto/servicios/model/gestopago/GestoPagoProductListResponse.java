package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta que representa el catálogo de productos de GestoPago
 * ya convertido de XML a JSON.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoProductListResponse {

    private String status;
    private String mensaje;
    private LocalDateTime fechaConsulta;
    private List<CatProductXml.ProductoItem> products;
}
