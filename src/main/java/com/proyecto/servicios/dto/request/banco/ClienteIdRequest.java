package com.proyecto.servicios.dto.request.banco;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Identificador de cliente recibido en el cuerpo de la petición (sustituye @PathVariable).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteIdRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser un número positivo")
    private Long id;
}
