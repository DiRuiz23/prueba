package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.RecursoDuplicadoException;

/**
 * El RFC ya esta registrado.
 */
public class RfcDuplicadoException extends RecursoDuplicadoException {

    public RfcDuplicadoException(String mensaje) {
        super(mensaje);
    }
}