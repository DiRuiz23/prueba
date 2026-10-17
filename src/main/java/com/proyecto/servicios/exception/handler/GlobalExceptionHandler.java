package com.proyecto.servicios.exception.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.NoAutenticadoException;
import com.proyecto.servicios.exception.RecursoDuplicadoException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones. Traduce cada tipo de excepción a un código HTTP
 * y a la estructura uniforme {@link GenericResponse}, sin exponer trazas, SQL ni
 * detalles internos al cliente (estos solo se registran en el log del servidor).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─────────────────────────────────────────────────────────────────────────
    // 400 BAD REQUEST — Validación y formato de la petición
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * En caso de que alguna validación de Bean Validation no se cumpla para algún DTO anotado con
     * {@code @Valid} se lanza {@link MethodArgumentNotValidException}; este método la atrapa y regresa
     * el mensaje configurado para cada campo inválido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Void>> manejarValidacionDto(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errores.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return responder(HttpStatus.BAD_REQUEST,
                GenericResponse.errorValidacion("Datos no válidos", errores));
    }

    /** Violaciones de restricciones validadas fuera del cuerpo (por ejemplo, en servicios con {@code @Validated}). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse<Void>> manejarRestriccionesVioladas(ConstraintViolationException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errores.putIfAbsent(
                v.getPropertyPath().toString(), v.getMessage()));
        return responder(HttpStatus.BAD_REQUEST,
                GenericResponse.errorValidacion("Datos no válidos", errores));
    }

    /**
     * El cuerpo no pudo convertirse al DTO esperado: JSON mal formado, tipo de dato incorrecto,
     * valor de enumeración inexistente o campo no permitido (por ejemplo, campos controlados por el servidor).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse<Void>> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getCause();

        if (causa instanceof UnrecognizedPropertyException e) {
            return responder(HttpStatus.BAD_REQUEST, GenericResponse.errorValidacion("Datos no válidos",
                    Map.of(e.getPropertyName(), "El campo no está permitido en esta operación")));
        }
        if (causa instanceof InvalidFormatException e) {
            String campo = rutaCampo(e);
            String detalle = e.getTargetType() != null && e.getTargetType().isEnum()
                    ? "Valor inválido. Valores permitidos: " + valoresEnum(e.getTargetType())
                    : "Formato inválido para el tipo de dato esperado";
            return responder(HttpStatus.BAD_REQUEST,
                    GenericResponse.errorValidacion("Datos no válidos", Map.of(campo, detalle)));
        }
        if (causa instanceof MismatchedInputException e && !e.getPath().isEmpty()) {
            return responder(HttpStatus.BAD_REQUEST, GenericResponse.errorValidacion("Datos no válidos",
                    Map.of(rutaCampo(e), "Tipo de dato incorrecto")));
        }
        return responder(HttpStatus.BAD_REQUEST,
                GenericResponse.error("El cuerpo de la petición está vacío o no es un JSON válido"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 401 / 403 — Autenticación y autorización
    // ─────────────────────────────────────────────────────────────────────────

    /** Credenciales incorrectas u otros fallos de identificación propios del negocio. */
    @ExceptionHandler(NoAutenticadoException.class)
    public ResponseEntity<GenericResponse<Void>> manejarNoAutenticado(NoAutenticadoException ex) {
        return responder(HttpStatus.UNAUTHORIZED, GenericResponse.error(ex.getMessage()));
    }

    /** Fallos de autenticación lanzados por Spring Security dentro de un controlador. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GenericResponse<Void>> manejarAutenticacionSpring(AuthenticationException ex) {
        return responder(HttpStatus.UNAUTHORIZED,
                GenericResponse.error("Se requiere un token de acceso válido"));
    }

    /** Usuario inactivo, cuenta bloqueada u otras restricciones de acceso del negocio. */
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<GenericResponse<Void>> manejarAccesoDenegado(AccesoDenegadoException ex) {
        return responder(HttpStatus.FORBIDDEN, GenericResponse.error(ex.getMessage()));
    }

    /** Falta de permisos detectada por Spring Security. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GenericResponse<Void>> manejarAccesoDenegadoSpring(AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN,
                GenericResponse.error("No cuenta con permisos para realizar esta operación"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 404 / 405 / 415 — Recurso, método y tipo de contenido
    // ─────────────────────────────────────────────────────────────────────────

    /** Cliente, cuenta, usuario, persona o catálogo inexistente. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<GenericResponse<Void>> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, GenericResponse.error(ex.getMessage()));
    }

    /** La ruta solicitada no existe en la API. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GenericResponse<Void>> manejarRutaInexistente(NoResourceFoundException ex) {
        return responder(HttpStatus.NOT_FOUND,
                GenericResponse.error("La ruta '/" + ex.getResourcePath() + "' no existe"));
    }

    /** La ruta existe pero no admite el método HTTP utilizado. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericResponse<Void>> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        String permitidos = ex.getSupportedHttpMethods() == null ? "" : ex.getSupportedHttpMethods().stream()
                .map(Object::toString).collect(Collectors.joining(", "));
        return responder(HttpStatus.METHOD_NOT_ALLOWED, GenericResponse.error(
                "El método " + ex.getMethod() + " no está soportado para esta ruta. Métodos permitidos: " + permitidos));
    }

    /** El cliente no envió el cuerpo como JSON. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GenericResponse<Void>> manejarTipoContenido(HttpMediaTypeNotSupportedException ex) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                GenericResponse.error("El encabezado Content-Type debe ser 'application/json'"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 409 CONFLICT
    // ─────────────────────────────────────────────────────────────────────────

    /** CURP, RFC, correo o usuario ya registrados. */
    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<GenericResponse<Void>> manejarRecursoDuplicado(RecursoDuplicadoException ex) {
        return responder(HttpStatus.CONFLICT, GenericResponse.error(ex.getMessage()));
    }

    /**
     * Restricción de base de datos violada (por ejemplo, una condición de carrera sobre un campo único).
     * El detalle SQL se registra en el log, nunca se envía al cliente.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse<Void>> manejarIntegridadDatos(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, GenericResponse.error(
                "La operación entra en conflicto con información ya registrada"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 422 UNPROCESSABLE ENTITY
    // ─────────────────────────────────────────────────────────────────────────

    /** Mayoría de edad, catálogos, estatus de cuenta y demás reglas de negocio. */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<GenericResponse<Void>> manejarReglaNegocio(ReglaNegocioException ex) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, GenericResponse.error(ex.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 500 INTERNAL SERVER ERROR — Último recurso
    // ─────────────────────────────────────────────────────────────────────────

    /** Cualquier error no previsto: se registra completo en el log y se responde sin detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Void>> manejarErrorInesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, GenericResponse.error(
                "Ocurrió un error interno al procesar la solicitud. Intente nuevamente más tarde"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUXILIARES
    // ─────────────────────────────────────────────────────────────────────────

    private ResponseEntity<GenericResponse<Void>> responder(HttpStatus status, GenericResponse<Void> cuerpo) {
        return ResponseEntity.status(status).body(cuerpo);
    }

    private String rutaCampo(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."))
                .replace(".[", "[");
    }

    private String valoresEnum(Class<?> tipoEnum) {
        return Arrays.stream(tipoEnum.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }
}
