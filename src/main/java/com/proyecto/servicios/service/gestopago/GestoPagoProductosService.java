package com.proyecto.servicios.service.gestopago;

import com.proyecto.servicios.dto.response.gestopago.GestoPagoProductListResponse;

/**
 * Contrato del servicio de catálogo de productos de GestoPago.
 */
public interface GestoPagoProductosService {

    /**
     * Retorna el catálogo de productos más reciente almacenado en MongoDB.
     */
    GestoPagoProductListResponse obtenerCatalogoProductos();
}
