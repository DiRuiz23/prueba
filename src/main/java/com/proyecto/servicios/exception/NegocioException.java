package com.proyecto.servicios.exception;

/**
 * Clase base de todas las excepciones de negocio. Sus subclases se traducen a respuestas HTTP en el GlobalExceptionHandler.
 */
public abstract class NegocioException extends RuntimeException {

    protected NegocioException(String mensaje) {
        super(mensaje);
    }
}