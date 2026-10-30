package com.proyecto.servicios.service.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;

import java.util.Optional;

public interface GestoPagoTokenService {

    void renovarToken();

    Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo);

    /**
     * Obtiene un token Bearer válido para transacciones. Si no existe o está vencido,
     * realiza la autenticación inicial.
     */
    String obtenerTokenBearerValido();

    /**
     * Fuerza la renovación inmediata del token con el proveedor mediante sincronización
     * de un solo vuelo (single-flight) ante respuestas 403 EXPIRED.
     */
    String forzarRenovacionYObtenerToken();
}
