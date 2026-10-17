package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.RecursoDuplicadoException;

/**
 * El cliente ya tiene un usuario de acceso asociado.
 */
public class UsuarioYaAsignadoException extends RecursoDuplicadoException {

    public UsuarioYaAsignadoException(String mensaje) {
        super(mensaje);
    }
}