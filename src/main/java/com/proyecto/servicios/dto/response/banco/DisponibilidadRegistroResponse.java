package com.proyecto.servicios.dto.response.banco;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadRegistroResponse {
    private boolean curpDisponible;
    private boolean rfcDisponible;
    private boolean correoDisponible;
    private boolean telefonoDisponible = true;

    public DisponibilidadRegistroResponse(boolean curpDisponible, boolean rfcDisponible, boolean correoDisponible) {
        this(curpDisponible, rfcDisponible, correoDisponible, true);
    }
}
