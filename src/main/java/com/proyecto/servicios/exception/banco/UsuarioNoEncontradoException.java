package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.RecursoNoEncontradoException;

/**
 * No existe el usuario de acceso solicitado.
 */
public class UsuarioNoEncontradoException extends RecursoNoEncontradoException {

    public UsuarioNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}