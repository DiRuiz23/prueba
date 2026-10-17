package com.proyecto.servicios.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Estructura uniforme para TODAS las respuestas de la API (éxito y error).
 * <ul>
 *   <li>{@code codigo}: 0 = operación exitosa, 1 = error.</li>
 *   <li>{@code mensaje}: descripción legible del resultado.</li>
 *   <li>{@code datos}: información devuelta (solo en respuestas exitosas).</li>
 *   <li>{@code errores}: detalle campo → mensaje (solo en errores de validación).</li>
 * </ul>
 * El estado real de la operación se comunica con el código HTTP; los campos nulos no se serializan.
 *
 * @param <T> tipo de la información devuelta en {@code datos}
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse<T> {

    public static final int CODIGO_EXITO = 0;
    public static final int CODIGO_ERROR = 1;

    private Integer codigo;
    private String mensaje;
    private T datos;
    private Map<String, String> errores;

    /** Respuesta exitosa con información en {@code datos}. */
    public static <T> GenericResponse<T> exito(String mensaje, T datos) {
        GenericResponse<T> respuesta = new GenericResponse<>();
        respuesta.setCodigo(CODIGO_EXITO);
        respuesta.setMensaje(mensaje);
        respuesta.setDatos(datos);
        return respuesta;
    }

    /** Respuesta exitosa que solo informa el resultado (sin {@code datos}). */
    public static GenericResponse<Void> exitoSinDatos(String mensaje) {
        return exito(mensaje, null);
    }

    /** Respuesta de error de negocio o técnico, sin detalle por campo. */
    public static GenericResponse<Void> error(String mensaje) {
        GenericResponse<Void> respuesta = new GenericResponse<>();
        respuesta.setCodigo(CODIGO_ERROR);
        respuesta.setMensaje(mensaje);
        return respuesta;
    }

    /** Respuesta de error de validación con el detalle campo → mensaje. */
    public static GenericResponse<Void> errorValidacion(String mensaje, Map<String, String> errores) {
        GenericResponse<Void> respuesta = error(mensaje);
        respuesta.setErrores(errores);
        return respuesta;
    }
}
