package com.proyecto.servicios.exception;

/**
 * El recurso solicitado no existe (HTTP 404).
 */
public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}