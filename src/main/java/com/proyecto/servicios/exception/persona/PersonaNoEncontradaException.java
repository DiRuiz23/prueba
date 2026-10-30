package com.proyecto.servicios.exception.persona;

import com.proyecto.servicios.exception.RecursoNoEncontradoException;

/**
 * No existe la persona solicitada.
 */
public class PersonaNoEncontradaException extends RecursoNoEncontradoException {

    public PersonaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}