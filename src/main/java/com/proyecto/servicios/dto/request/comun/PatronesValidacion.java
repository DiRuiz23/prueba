package com.proyecto.servicios.dto.request.comun;

/**
 * Expresiones regulares reutilizadas por las anotaciones {@code @Pattern} de los DTOs.
 * Centralizarlas evita que una misma regla quede definida de forma distinta en cada request.
 */
public final class PatronesValidacion {

    /** Letras (incluye acentos y ñ) y espacios, entre 2 y 50 caracteres. */
    public static final String NOMBRE = "^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\\s]{2,50}$";

    /** CURP de 18 caracteres (4 letras + 6 dígitos + H/M + 5 letras + 2 homoclave). */
    public static final String CURP = "^(?i)[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[A-Z0-9]{2}$";

    /** RFC de persona física o moral (3-4 letras + 6 dígitos + 3 homoclave). */
    public static final String RFC = "^(?i)[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}$";

    /** Teléfono nacional de 10 dígitos. */
    public static final String TELEFONO = "^[0-9]{10}$";

    /** Código postal mexicano de 5 dígitos. */
    public static final String CODIGO_POSTAL = "^[0-9]{5}$";

    /** Número exterior/interior de domicilio: letras, números, guion o diagonal (1 a 6 caracteres). */
    public static final String NUMERO_DOMICILIO = "^[A-Za-z0-9/\\-]{1,6}$";

    /** Número interior opcional de domicilio (vacío o 1 a 6 caracteres alfanuméricos/guion/diagonal). */
    public static final String NUMERO_DOMICILIO_OPCIONAL = "^$|^[A-Za-z0-9/\\-]{1,6}$";

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
