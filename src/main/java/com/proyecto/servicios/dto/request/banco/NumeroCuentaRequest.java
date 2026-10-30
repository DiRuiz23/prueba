package com.proyecto.servicios.dto.request.banco;

import com.proyecto.servicios.dto.request.comun.PatronesValidacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Número de cuenta recibido en el cuerpo de la petición (sustituye @PathVariable).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NumeroCuentaRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = "El número de cuenta debe contener exactamente 13 dígitos numéricos")
    private String numeroCuenta;
}
