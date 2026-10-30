package com.proyecto.servicios.exception.gestopago;

import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.exception.NegocioException;

public class ResultadoInciertoException extends NegocioException {

    private final TransaccionServicio transaccion;

    public ResultadoInciertoException(String mensaje, TransaccionServicio transaccion) {
        super(mensaje);
        this.transaccion = transaccion;
    }

    public TransaccionServicio getTransaccion() {
        return transaccion;
    }
}

