package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.NoAutenticadoException;

/**
 * Las credenciales de acceso son incorrectas.
 */
public class CredencialesInvalidasException extends NoAutenticadoException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}