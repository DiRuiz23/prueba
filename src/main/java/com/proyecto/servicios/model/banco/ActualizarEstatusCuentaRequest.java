package com.proyecto.servicios.model.banco;

import com.proyecto.servicios.entity.banco.Cuenta.EstatusCuenta;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActualizarEstatusCuentaRequest {
    @NotNull(message = "El nuevo estatus es obligatorio (ACTIVA, BLOQUEADA, INACTIVA)")
    private EstatusCuenta nuevoEstatus;

    private String motivo;
}
