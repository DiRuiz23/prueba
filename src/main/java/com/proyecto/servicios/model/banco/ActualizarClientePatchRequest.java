package com.proyecto.servicios.model.banco;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para actualización parcial. Note que CURP, RFC y número de cuenta
 * NO están presentes aquí de forma intencional para garantizar inmutabilidad.
 */
@Data
public class ActualizarClientePatchRequest {
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,50}$", message = "Nombre inválido")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,50}$", message = "Segundo nombre inválido")
    private String segundoNombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,50}$", message = "Apellido paterno inválido")
    private String apellidoPaterno;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,50}$", message = "Apellido materno inválido")
    private String apellidoMaterno;

    @Pattern(regexp = "^(?i)(H|M|Indefinido)$", message = "El sexo debe ser 'H', 'M' o 'Indefinido'")
    private String sexo;
    private String estadoCivil;
    @Pattern(regexp = "^(?i)(Mexicana)$", message = "Por el momento solo se acepta nacionalidad 'Mexicana'")
    private String nacionalidad;

    @Email(message = "Correo inválido")
    @Size(max = 100)
    private String correo;

    @Pattern(regexp = "^[0-9]{10}$", message = "Teléfono móvil debe tener 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = "^[0-9]{10}$", message = "Teléfono alternativo debe tener 10 dígitos")
    private String telefonoAlternativo;

    private String ocupacion;
    private String empresa;

    @DecimalMin(value = "0.01", message = "El ingreso debe ser positivo")
    private BigDecimal ingresoMensual;

    private DomicilioDTO domicilio;
}
