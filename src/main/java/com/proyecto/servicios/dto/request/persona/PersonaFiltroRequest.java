package com.proyecto.servicios.dto.request.persona;

import com.proyecto.servicios.dto.request.comun.PaginacionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PersonaFiltroRequest {

    @Size(max = 50, message = "El nombre no debe exceder 50 caracteres")
    private String nombre;

    @Size(max = 50, message = "El apellido paterno no debe exceder 50 caracteres")
    private String apellidoPaterno;

    @Size(max = 50, message = "El apellido materno no debe exceder 50 caracteres")
    private String apellidoMaterno;

    @Pattern(regexp = "^(?i)(id|nombre|apellidoPaterno|apellidoMaterno)$",
            message = "Campo de ordenamiento inválido. Permitidos: id, nombre, apellidoPaterno, apellidoMaterno")
    private String ordenarPor;

    @Valid
    private PaginacionRequest paginacion = new PaginacionRequest();
}
