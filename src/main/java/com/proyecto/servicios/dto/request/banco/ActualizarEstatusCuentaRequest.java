package com.proyecto.servicios.dto.request.banco;

import com.proyecto.servicios.dto.request.comun.PatronesValidacion;
import com.proyecto.servicios.enums.EstatusCuenta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActualizarEstatusCuentaRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = "El número de cuenta debe contener exactamente 13 dígitos numéricos")
    private String numeroCuenta;

    @NotNull(message = "El nuevo estatus es obligatorio (ACTIVA, BLOQUEADA, INACTIVA)")
    private EstatusCuenta nuevoEstatus;

    @Size(max = 255, message = "El motivo no debe exceder 255 caracteres")
    private String motivo;
}
