package com.proyecto.servicios.dto.request.banco;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Petición para realizar pago de servicios o recarga electrónica debitando de cuenta bancaria")
public class PagoServicioRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Size(min = 10, max = 20, message = "El número de cuenta debe tener entre 10 y 20 caracteres")
    @Schema(description = "Número de cuenta bancaria de donde se debitarán los fondos", example = "01200000000000000016")
    private String numeroCuenta;

    @NotNull(message = "El ID del producto es obligatorio")
    @Schema(description = "Identificador del producto en GestoPago", example = "582")
    private Integer idProducto;

    @NotNull(message = "El ID del servicio es obligatorio")
    @Schema(description = "Identificador del servicio en GestoPago", example = "133")
    private Integer idServicio;

    @NotBlank(message = "El teléfono o referencia es obligatorio")
    @Size(min = 8, max = 100, message = "El teléfono o referencia debe tener entre 8 y 100 caracteres")
    @Schema(description = "Número de teléfono a recargar o referencia del recibo de servicio", example = "5555555551")
    private String telefonoReferencia;

    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Digits(integer = 13, fraction = 2, message = "El monto admite hasta 13 enteros y 2 decimales")
    @Schema(description = "Monto a pagar (opcional para productos con precio fijo en catálogo)", example = "10.00")
    private BigDecimal monto;

    @NotBlank(message = "El PIN o contraseña de autorización es obligatorio")
    @Schema(description = "PIN de seguridad de la cuenta o contraseña del usuario para autorizar el cargo", example = "Password123!")
    private String pin;

    @Schema(description = "Bandera o parámetro adicional de operación", example = "0")
    private String bandera;
}
