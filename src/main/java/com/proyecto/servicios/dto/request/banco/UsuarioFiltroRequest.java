package com.proyecto.servicios.dto.request.banco;

import com.proyecto.servicios.dto.request.comun.PaginacionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Criterios de búsqueda dinámica para usuarios de acceso.
 */
@Data
public class UsuarioFiltroRequest {

    @Size(max = 100, message = "El correo no debe exceder 100 caracteres")
    private String correo;

    private Boolean activo;

    @Positive(message = "El ID del cliente debe ser un número positivo")
    private Long clienteId;

    @Pattern(regexp = "^(?i)(id|correo|activo|fechaCreacion)$",
            message = "Campo de ordenamiento inválido. Permitidos: id, correo, activo, fechaCreacion")
    private String ordenarPor;

    @Valid
    private PaginacionRequest paginacion = new PaginacionRequest();
}
