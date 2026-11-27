package com.proyecto.servicios.exception.gestopago;

import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.exception.NegocioException;

public class ProveedorException extends NegocioException {

    private final String codigoRespuesta;
    private final TransaccionServicio transaccion;

    public ProveedorException(String mensaje, String codigoRespuesta) {
        this(mensaje, codigoRespuesta, null);
    }

    public ProveedorException(String mensaje, String codigoRespuesta, TransaccionServicio transaccion) {
        super(mensaje);
        this.codigoRespuesta = codigoRespuesta;
        this.transaccion = transaccion;
    }

    public String getCodigoRespuesta() {
        return codigoRespuesta;
    }

    public TransaccionServicio getTransaccion() {
        return transaccion;
    }
}

