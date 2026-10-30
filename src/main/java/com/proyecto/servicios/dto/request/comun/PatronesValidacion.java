package com.proyecto.servicios.dto.request.comun;

/**
 * Expresiones regulares reutilizadas por las anotaciones {@code @Pattern} de los DTOs.
 * Centralizarlas evita que una misma regla quede definida de forma distinta en cada request.
 */
public final class PatronesValidacion {

    /** Letras (incluye acentos y ñ) y espacios, entre 2 y 50 caracteres. */
    public static final String NOMBRE = "^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\\s]{2,50}$";

    /** CURP oficial de 18 caracteres. */
    public static final String CURP = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9A-Z]{2}$";

    /** RFC de persona física o moral (12 o 13 caracteres). */
    public static final String RFC = "^[A-Z&Ñ]{3,4}[0-9]{6}[A-V1-9][A-Z1-9][0-9A-Z]$";

    /** Teléfono nacional de 10 dígitos. */
    public static final String TELEFONO = "^[0-9]{10}$";

    /** Código postal mexicano de 5 dígitos. */
    public static final String CODIGO_POSTAL = "^[0-9]{5}$";

    /** Número de cuenta generado por el sistema: prefijo bancario de 3 dígitos + 10 dígitos. */
    public static final String NUMERO_CUENTA = "^[0-9]{13}$";

    /** Mínimo 8 caracteres con mayúscula, minúscula, número y carácter especial. */
    public static final String PASSWORD = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).{8,}$";

    /** Valores del enum EstadoCivil (sin distinguir mayúsculas). */
    public static final String ESTADO_CIVIL = "^(?i)(SOLTERO|CASADO|DIVORCIADO|VIUDO|UNION_LIBRE)$";

    /** Sexo permitido por el sistema. */
    public static final String SEXO = "^(?i)(H|M|Indefinido)$";

    /** Nacionalidad aceptada actualmente. */
    public static final String NACIONALIDAD = "^(?i)(Mexicana)$";

    private PatronesValidacion() {
    }
}
