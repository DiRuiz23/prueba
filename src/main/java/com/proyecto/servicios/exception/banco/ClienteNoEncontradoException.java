package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.RecursoNoEncontradoException;

/**
 * No existe el cliente solicitado.
 */
public class ClienteNoEncontradoException extends RecursoNoEncontradoException {

    public ClienteNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}