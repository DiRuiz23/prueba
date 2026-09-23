package com.proyecto.servicios.enums;

/**
 * Enumeración de mensajes de error para la integración con GestoPago.
 * Centraliza los mensajes de error para evitar strings literales dispersos en el código.
 */
public enum GestoPagoEnum {

    TOKEN_NO_ENCONTRADO("No se encontró token activo de GestoPago"),
    ERROR_COMUNICACION("Error de comunicación con el servicio GestoPago"),
    ERROR_AUTENTICACION("Token GestoPago inválido o expirado"),
    ERROR_SERVICIO_EXTERNO("El servicio GestoPago respondió con error"),
    RESPUESTA_INVALIDA("La respuesta del servicio GestoPago es inválida"),
    ERROR_PARSEO_XML("Error al parsear la respuesta XML de GestoPago"),
    SIN_CATALOGO("No existe catálogo de productos disponible");

    private final String mensaje;

    GestoPagoEnum(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
