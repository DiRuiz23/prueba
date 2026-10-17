package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.ReglaNegocioException;

/**
 * La contrasena no cumple las reglas de complejidad.
 */
public class ContrasenaInvalidaException extends ReglaNegocioException {

    public ContrasenaInvalidaException(String mensaje) {
        super(mensaje);
    }
}