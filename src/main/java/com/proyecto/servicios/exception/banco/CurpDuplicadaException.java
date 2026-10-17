package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.RecursoDuplicadoException;

/**
 * La CURP ya esta registrada.
 */
public class CurpDuplicadaException extends RecursoDuplicadoException {

    public CurpDuplicadaException(String mensaje) {
        super(mensaje);
    }
}