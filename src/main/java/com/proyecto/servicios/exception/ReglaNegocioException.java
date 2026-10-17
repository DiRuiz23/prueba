package com.proyecto.servicios.exception;

/**
 * La operacion viola una regla de negocio del sistema (HTTP 422).
 */
public class ReglaNegocioException extends NegocioException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}