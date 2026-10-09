package com.proyecto.servicios.model.banco;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RegistroClienteRequest {

    // --- DATOS PERSONALES ---
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,50}$", message = "El nombre solo debe contener letras y tener entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,50}$", message = "El segundo nombre es inválido")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,50}$", message = "El apellido paterno solo debe contener letras y tener entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,50}$", message = "El apellido materno solo debe contener letras y tener entre 2 y 50 caracteres")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe estar en el pasado")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9A-Z]{2}$", message = "El formato de CURP es inválido (18 caracteres oficiales)")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-Z&Ñ]{3,4}[0-9]{6}[A-V1-9][A-Z1-9][0-9A-Z]$", message = "El formato de RFC es inválido (12 o 13 caracteres)")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^(?i)(H|M|Indefinido)$", message = "El sexo debe ser 'H', 'M' o 'Indefinido'")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Pattern(regexp = "^(?i)(Mexicana)$", message = "Por el momento solo se acepta nacionalidad 'Mexicana'")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    private String estadoCivil;

    // --- DATOS DE CONTACTO ---
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo es inválido")
    @Size(max = 100, message = "El correo no debe exceder los 100 caracteres")
    private String correo;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos numéricos")
    private String telefonoMovil;

    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono alternativo debe contener exactamente 10 dígitos numéricos")
    private String telefonoAlternativo;

    // --- INFORMACIÓN LABORAL ---
    @NotBlank(message = "La ocupación es obligatoria")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    // --- DOMICILIO ASOCIADO ---
    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioDTO domicilio;

    // --- CREDENCIALES DE ACCESO ---
    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).{8,}$", message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial")
    private String password;

    // --- BIOMETRÍA FACIAL ---
    @NotBlank(message = "La captura facial biométrica es obligatoria")
    private String datosBiometricosFacialBase64;
}