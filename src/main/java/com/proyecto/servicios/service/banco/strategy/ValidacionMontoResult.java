package com.proyecto.servicios.service.banco.strategy;

import java.math.BigDecimal;

public record ValidacionMontoResult(
        BigDecimal montoOperacion,
        BigDecimal comision,
        BigDecimal montoDebitadoTotal,
        Integer tipoFront
) {
    public ValidacionMontoResult(
            BigDecimal montoOperacion,
            BigDecimal comision,
            BigDecimal montoDebitadoTotal
    ) {
        this(montoOperacion, comision, montoDebitadoTotal, 1);
    }
}

