package com.proyecto.servicios.service.banco.strategy;

import com.proyecto.servicios.client.model.CatProductXml.ProductoItem;
import com.proyecto.servicios.exception.banco.MontoInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PagoServicioStrategy implements ReglaMontoStrategy {

    private final BigDecimal maxPorTransaccion;

    public PagoServicioStrategy(
            @Value("${banco.pagos.max-por-transaccion:5000.00}") BigDecimal maxPorTransaccion
    ) {
        this.maxPorTransaccion = maxPorTransaccion;
    }

    @Override
    public boolean soporta(Integer tipoFront) {
        return tipoFront != null && tipoFront == 2;
    }

    @Override
    public ValidacionMontoResult validarYCalcular(BigDecimal montoRequest, ProductoItem producto) {
        if (montoRequest == null) {
            throw new MontoInvalidoException("El monto a pagar del servicio es obligatorio.");
        }

        if (montoRequest.compareTo(new BigDecimal("0.01")) < 0) {
            throw new MontoInvalidoException("El monto a pagar debe ser de al menos $0.01.");
        }

        if (montoRequest.compareTo(maxPorTransaccion) > 0) {
            throw new MontoInvalidoException(String.format(
                    "El monto solicitado ($%s) supera el límite máximo permitido por transacción ($%s).",
                    montoRequest.toPlainString(), maxPorTransaccion.toPlainString()
            ));
        }

        // En tipoFront = 2, el campo 'precio' del catálogo representa la comisión del servicio
        BigDecimal comision = BigDecimal.ZERO;
        if (producto.getPrecio() != null && !producto.getPrecio().isBlank()) {
            try {
                BigDecimal valor = new BigDecimal(producto.getPrecio().trim());
                if (valor.compareTo(BigDecimal.ZERO) >= 0) {
                    comision = valor;
                }
            } catch (NumberFormatException ignored) {
                comision = BigDecimal.ZERO;
            }
        }

        BigDecimal totalDebito = montoRequest.add(comision);
        Integer tipoFront = producto.getTipoFront() != null ? producto.getTipoFront() : 2;
        return new ValidacionMontoResult(montoRequest, comision, totalDebito, tipoFront);
    }

    @Override
    public void validarReferencia(String telefonoReferencia, ProductoItem producto) {
        if (telefonoReferencia == null || telefonoReferencia.trim().isEmpty()) {
            throw new MontoInvalidoException("La referencia del recibo o servicio es obligatoria.");
        }

        String refLimpia = telefonoReferencia.trim();
        if (refLimpia.length() > 100) {
            throw new MontoInvalidoException("La referencia del servicio excede la longitud máxima permitida de 100 caracteres.");
        }
    }
}
