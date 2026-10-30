package com.proyecto.servicios.exception;

/**
 * El solicitante esta identificado pero no puede realizar la operacion (HTTP 403).
 */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}