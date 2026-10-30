package com.proyecto.servicios.exception.gestopago;

import com.proyecto.servicios.exception.NegocioException;

public class ProveedorNoDisponibleException extends NegocioException {
    public ProveedorNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public ProveedorNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

