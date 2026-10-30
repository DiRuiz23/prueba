package com.proyecto.servicios.dto.request.banco;

import com.proyecto.servicios.dto.request.comun.PaginacionRequest;
import com.proyecto.servicios.dto.request.comun.PatronesValidacion;
import com.proyecto.servicios.enums.EstatusCuenta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Criterios de búsqueda dinámica para cuentas bancarias.
 * Permite filtrar por cliente, estatus, rango de saldo, etc. de manera combinable.
 */
@Data
public class CuentaFiltroRequest {

    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = "El número de cuenta debe tener exactamente 13 dígitos")
    private String numeroCuenta;

    @Positive(message = "El ID del cliente debe ser un número positivo")
    private Long clienteId;

    private EstatusCuenta estatus;

    private BigDecimal saldoMinimo;

    private BigDecimal saldoMaximo;

    @Pattern(regexp = "^(?i)(id|numeroCuenta|saldo|fechaCreacion|estatus)$",
            message = "Campo de ordenamiento inválido. Permitidos: id, numeroCuenta, saldo, fechaCreacion, estatus")
    private String ordenarPor;

    @Valid
    private PaginacionRequest paginacion = new PaginacionRequest();
}
