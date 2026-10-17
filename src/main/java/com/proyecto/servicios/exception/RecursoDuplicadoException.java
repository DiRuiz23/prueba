package com.proyecto.servicios.exception;

/**
 * El recurso que se intenta registrar ya existe (HTTP 409).
 */
public class RecursoDuplicadoException extends NegocioException {

    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}