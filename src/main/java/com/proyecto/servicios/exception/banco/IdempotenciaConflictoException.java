package com.proyecto.servicios.exception.banco;

import com.proyecto.servicios.exception.RecursoDuplicadoException;

public class IdempotenciaConflictoException extends RecursoDuplicadoException {
    public IdempotenciaConflictoException(String mensaje) {
        super(mensaje);
    }
}

