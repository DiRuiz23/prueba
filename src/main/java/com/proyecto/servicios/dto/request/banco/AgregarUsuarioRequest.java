package com.proyecto.servicios.dto.request.banco;

import com.proyecto.servicios.dto.request.comun.PatronesValidacion;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request para crear o asociar un usuario de acceso a un cliente existente.
 */
@Data
public class AgregarUsuarioRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser un número positivo")
    private Long clienteId;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo es inválido")
    @Size(max = 100, message = "El correo no debe exceder los 100 caracteres")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(
            regexp = PatronesValidacion.PASSWORD,
            message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial"
    )
    private String password;
}
