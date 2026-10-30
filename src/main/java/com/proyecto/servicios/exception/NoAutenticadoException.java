package com.proyecto.servicios.exception;

/**
 * La identidad del solicitante no pudo verificarse (HTTP 401).
 */
public class NoAutenticadoException extends NegocioException {

    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}