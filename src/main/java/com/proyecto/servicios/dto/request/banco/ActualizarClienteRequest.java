package com.proyecto.servicios.dto.request.banco;

import com.proyecto.servicios.dto.request.comun.PatronesValidacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para actualización de datos del cliente (PATCH/PUT).
 * CURP, RFC y número de cuenta son inmutables y no se incluyen aquí.
 */
@Data
public class ActualizarClienteRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser un número positivo")
    private Long id;

    @Pattern(regexp = PatronesValidacion.NOMBRE, message = "El nombre solo debe contener letras y tener entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = PatronesValidacion.NOMBRE, message = "El segundo nombre solo debe contener letras y tener entre 2 y 50 caracteres")
    private String segundoNombre;

    @Pattern(regexp = PatronesValidacion.NOMBRE, message = "El apellido paterno solo debe contener letras y tener entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @Pattern(regexp = PatronesValidacion.NOMBRE, message = "El apellido materno solo debe contener letras y tener entre 2 y 50 caracteres")
    private String apellidoMaterno;

    @Pattern(regexp = PatronesValidacion.SEXO, message = "El sexo debe ser 'H', 'M' o 'Indefinido'")
    private String sexo;

    @Pattern(regexp = PatronesValidacion.ESTADO_CIVIL, message = "Estado civil inválido. Valores permitidos: SOLTERO, CASADO, DIVORCIADO, VIUDO, UNION_LIBRE")
    private String estadoCivil;

    @Pattern(regexp = PatronesValidacion.NACIONALIDAD, message = "Por el momento solo se acepta nacionalidad 'Mexicana'")
    private String nacionalidad;

    @Email(message = "El formato de correo es inválido")
    @Size(max = 100, message = "El correo no debe exceder los 100 caracteres")
    private String correo;

    @Pattern(regexp = PatronesValidacion.TELEFONO, message = "El teléfono móvil debe contener exactamente 10 dígitos numéricos")
    private String telefonoMovil;

    @Pattern(regexp = PatronesValidacion.TELEFONO, message = "El teléfono alternativo debe contener exactamente 10 dígitos numéricos")
    private String telefonoAlternativo;

    @Size(max = 100, message = "La ocupación no debe exceder los 100 caracteres")
    private String ocupacion;

    @Size(max = 100, message = "La empresa no debe exceder los 100 caracteres")
    private String empresa;

    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 13, fraction = 2, message = "El ingreso mensual admite hasta 13 enteros y 2 decimales")
    private BigDecimal ingresoMensual;

    @Valid
    private DomicilioRequest domicilio;
}
