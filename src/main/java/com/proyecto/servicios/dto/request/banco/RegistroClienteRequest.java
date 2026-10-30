package com.proyecto.servicios.dto.request.banco;

import com.proyecto.servicios.dto.request.comun.PatronesValidacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos para el onboarding de un cliente nuevo. No incluye campos controlados por el servidor
 * (id, activo, fechas de auditoría, número de cuenta, saldo): si el cliente los envía la petición se rechaza.
 */
@Data
public class RegistroClienteRequest {

    // --- DATOS PERSONALES ---
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = PatronesValidacion.NOMBRE, message = "El nombre solo debe contener letras y tener entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = PatronesValidacion.NOMBRE, message = "El segundo nombre solo debe contener letras y tener entre 2 y 50 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = PatronesValidacion.NOMBRE, message = "El apellido paterno solo debe contener letras y tener entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = PatronesValidacion.NOMBRE, message = "El apellido materno solo debe contener letras y tener entre 2 y 50 caracteres")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe estar en el pasado")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = PatronesValidacion.CURP, message = "El formato de CURP es inválido (18 caracteres oficiales en mayúsculas)")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = PatronesValidacion.RFC, message = "El formato de RFC es inválido (12 o 13 caracteres en mayúsculas)")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = PatronesValidacion.SEXO, message = "El sexo debe ser 'H', 'M' o 'Indefinido'")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Pattern(regexp = PatronesValidacion.NACIONALIDAD, message = "Por el momento solo se acepta nacionalidad 'Mexicana'")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(regexp = PatronesValidacion.ESTADO_CIVIL, message = "Estado civil inválido. Valores permitidos: SOLTERO, CASADO, DIVORCIADO, VIUDO, UNION_LIBRE")
    private String estadoCivil;

    // --- DATOS DE CONTACTO ---
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo es inválido")
    @Size(max = 100, message = "El correo no debe exceder los 100 caracteres")
    private String correo;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = PatronesValidacion.TELEFONO, message = "El teléfono móvil debe contener exactamente 10 dígitos numéricos")
    private String telefonoMovil;

    @Pattern(regexp = PatronesValidacion.TELEFONO, message = "El teléfono alternativo debe contener exactamente 10 dígitos numéricos")
    private String telefonoAlternativo;

    // --- INFORMACIÓN LABORAL ---
    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 100, message = "La ocupación no debe exceder los 100 caracteres")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 100, message = "La empresa no debe exceder los 100 caracteres")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 13, fraction = 2, message = "El ingreso mensual admite hasta 13 enteros y 2 decimales")
    private BigDecimal ingresoMensual;

    // --- DOMICILIO ASOCIADO ---
    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioRequest domicilio;

    // --- CREDENCIALES DE ACCESO ---
    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = PatronesValidacion.PASSWORD, message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial")
    private String password;

    // --- BIOMETRÍA FACIAL (OPCIONAL) ---
    private String datosBiometricosFacialBase64;
}