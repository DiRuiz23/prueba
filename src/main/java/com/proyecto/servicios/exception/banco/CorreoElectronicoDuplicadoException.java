package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.RecursoDuplicadoException;

/**
 * El correo electronico ya esta en uso.
 */
public class CorreoElectronicoDuplicadoException extends RecursoDuplicadoException {

    public CorreoElectronicoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}