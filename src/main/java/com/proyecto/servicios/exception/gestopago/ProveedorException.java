package com.proyecto.servicios.exception.gestopago;

import com.proyecto.servicios.exception.NegocioException;

public class ProveedorException extends NegocioException {

    private final String codigoRespuesta;

    public ProveedorException(String mensaje, String codigoRespuesta) {
        super(mensaje);
        this.codigoRespuesta = codigoRespuesta;
    }

    public String getCodigoRespuesta() {
        return codigoRespuesta;
    }
}

