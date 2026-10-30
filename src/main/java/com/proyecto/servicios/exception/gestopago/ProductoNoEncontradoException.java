package com.proyecto.servicios.exception.gestopago;

import com.proyecto.servicios.exception.RecursoNoEncontradoException;

public class ProductoNoEncontradoException extends RecursoNoEncontradoException {
    public ProductoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}

