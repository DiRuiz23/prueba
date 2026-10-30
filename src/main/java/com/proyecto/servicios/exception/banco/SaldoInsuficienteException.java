package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.ReglaNegocioException;

public class SaldoInsuficienteException extends ReglaNegocioException {
    public SaldoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}

