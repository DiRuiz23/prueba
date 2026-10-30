package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.banco.ContrasenaInvalidaException;

public class PinInvalidoException extends ContrasenaInvalidaException {
    public PinInvalidoException(String mensaje) {
        super(mensaje);
    }
}

