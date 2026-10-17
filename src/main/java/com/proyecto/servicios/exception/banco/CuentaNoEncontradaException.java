package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.RecursoNoEncontradoException;

/**
 * No existe la cuenta bancaria solicitada.
 */
public class CuentaNoEncontradaException extends RecursoNoEncontradoException {

    public CuentaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}