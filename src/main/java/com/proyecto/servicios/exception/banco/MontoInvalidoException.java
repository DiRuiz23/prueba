package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.ReglaNegocioException;

public class MontoInvalidoException extends ReglaNegocioException {
    public MontoInvalidoException(String mensaje) {
        super(mensaje);
    }
}

