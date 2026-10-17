package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.AccesoDenegadoException;

/**
 * El usuario esta inactivo y no puede autenticarse.
 */
public class UsuarioInactivoException extends AccesoDenegadoException {

    public UsuarioInactivoException(String mensaje) {
        super(mensaje);
    }
}