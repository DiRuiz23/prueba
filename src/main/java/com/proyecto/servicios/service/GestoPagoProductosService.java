package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;

/**
 * Contrato del servicio de catálogo de productos de GestoPago.
 */
public interface GestoPagoProductosService {

    /**
     * Retorna el catálogo de productos más reciente almacenado en MongoDB.
     */
    GestoPagoProductListResponse obtenerCatalogoProductos();
}
