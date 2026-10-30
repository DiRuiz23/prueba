package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.AccesoDenegadoException;

/**
 * La cuenta esta bloqueada y no admite la operacion.
 */
public class CuentaBloqueadaException extends AccesoDenegadoException {

    public CuentaBloqueadaException(String mensaje) {
        super(mensaje);
    }
}