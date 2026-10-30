package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.RecursoDuplicadoException;

/**
 * El cliente ya se encuentra registrado.
 */
public class ClienteYaRegistradoException extends RecursoDuplicadoException {

    public ClienteYaRegistradoException(String mensaje) {
        super(mensaje);
    }
}