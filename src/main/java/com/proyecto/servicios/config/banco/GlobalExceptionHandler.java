package com.proyecto.servicios.config.banco;

import com.proyecto.servicios.exception.banco.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─────────────────────────────────────────────────────────────────────────
    // 400 BAD REQUEST
    // ─────────────────────────────────────────────────────────────────────────

    /** Maneja errores de validación de campos (@Valid, @NotBlank, @Pattern, etc.) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de Validación de Datos", errores);
    }

    /** Maneja errores de JSON mal formados */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "El cuerpo de la petición está mal formado o es inválido.");
    }

    /** Contraseña que no cumple las reglas de complejidad. */
    @ExceptionHandler(ContrasenaInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleContrasenaInvalida(
            ContrasenaInvalidaException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 401 UNAUTHORIZED
    // ─────────────────────────────────────────────────────────────────────────

    /** Credenciales de acceso incorrectas. */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredencialesInvalidas(
            CredencialesInvalidasException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 403 FORBIDDEN
    // ─────────────────────────────────────────────────────────────────────────

    /** Cuenta bloqueada. */
    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<Map<String, Object>> handleCuentaBloqueada(
            CuentaBloqueadaException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** Usuario inactivo (no puede autenticarse). */
    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioInactivo(
            UsuarioInactivoException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 404 NOT FOUND
    // ─────────────────────────────────────────────────────────────────────────

    /** Cliente no encontrado. */
    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleClienteNoEncontrado(
            ClienteNoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Cuenta no encontrada. */
    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleCuentaNoEncontrada(
            CuentaNoEncontradaException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Usuario no encontrado. */
    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioNoEncontrado(
            UsuarioNoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Recurso genérico no encontrado (fallback para casos no cubiertos por
     * las excepciones específicas anteriores).
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrado(
            RecursoNoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 409 CONFLICT
    // ─────────────────────────────────────────────────────────────────────────

    /** CURP ya registrada en el sistema. */
    @ExceptionHandler(CurpDuplicadaException.class)
    public ResponseEntity<Map<String, Object>> handleCurpDuplicada(
            CurpDuplicadaException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** RFC ya registrado en el sistema. */
    @ExceptionHandler(RfcDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleRfcDuplicado(
            RfcDuplicadoException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Correo electrónico ya en uso. */
    @ExceptionHandler(CorreoElectronicoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleCorreoDuplicado(
            CorreoElectronicoDuplicadoException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Cliente ya registrado (duplicado genérico de cliente). */
    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<Map<String, Object>> handleClienteYaRegistrado(
            ClienteYaRegistradoException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Recurso duplicado genérico (fallback para casos no cubiertos
     * por las excepciones específicas anteriores).
     */
    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicado(
            RecursoDuplicadoException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 422 UNPROCESSABLE ENTITY
    // ─────────────────────────────────────────────────────────────────────────

    /** Regla de negocio violada (mayoría de edad, cuenta de cliente inactivo, etc.) */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleReglaNegocio(
            ReglaNegocioException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String error, Object detalles) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("detalles", detalles);
        return ResponseEntity.status(status).body(body);
    }
}
