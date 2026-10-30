package com.proyecto.servicios.exception.gestopago;

import com.proyecto.servicios.exception.RecursoNoEncontradoException;

/**
 * Aun no existe un catalogo de productos de GestoPago almacenado.
 */
public class CatalogoNoDisponibleException extends RecursoNoEncontradoException {

    public CatalogoNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}