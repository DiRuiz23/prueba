package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.RecursoDuplicadoException;

/**
 * El número de teléfono móvil ya pertenece a un cliente con cuenta activa.
 */
public class TelefonoDuplicadoException extends RecursoDuplicadoException {

    public TelefonoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
