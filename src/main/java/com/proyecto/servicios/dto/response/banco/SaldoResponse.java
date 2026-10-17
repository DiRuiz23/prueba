package com.proyecto.servicios.dto.response.banco;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Respuesta para consulta de saldo (evita devolver BigDecimal plano sin estructura DTO).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaldoResponse {

    private String numeroCuenta;
    private BigDecimal saldo;
}
