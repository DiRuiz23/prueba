package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.NegocioException;

public class PinBloqueadoException extends NegocioException {
    public PinBloqueadoException(String mensaje) {
        super(mensaje);
    }
}

