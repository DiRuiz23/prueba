package com.proyecto.servicios.service.banco.strategy;

import com.proyecto.servicios.client.model.CatProductXml.ProductoItem;
import com.proyecto.servicios.exception.banco.MontoInvalidoException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@Component
public class RecargaStrategy implements ReglaMontoStrategy {

    private static final Pattern TELEFONO_PATTERN = Pattern.compile("^[0-9]{10}$");

    @Override
    public boolean soporta(Integer tipoFront) {
        return tipoFront != null && tipoFront == 1;
    }

    @Override
    public ValidacionMontoResult validarYCalcular(BigDecimal montoRequest, ProductoItem producto) {
        if (montoRequest == null) {
            throw new MontoInvalidoException("El monto de la recarga es obligatorio.");
        }

        BigDecimal precioCatalogo;
        try {
            if (producto.getPrecio() == null || producto.getPrecio().isBlank()) {
                throw new MontoInvalidoException("El producto de recarga no tiene un precio definido en el catálogo.");
            }
            precioCatalogo = new BigDecimal(producto.getPrecio().trim());
        } catch (NumberFormatException e) {
            throw new MontoInvalidoException("El precio en catálogo del producto de recarga no es un número válido.");
        }

        if (precioCatalogo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MontoInvalidoException("El producto de recarga no tiene un precio válido en el catálogo.");
        }

        if (montoRequest.compareTo(precioCatalogo) != 0) {
            throw new MontoInvalidoException(String.format(
                    "El monto solicitado ($%s) no coincide con el valor exacto de la recarga en catálogo ($%s).",
                    montoRequest.toPlainString(), precioCatalogo.toPlainString()
            ));
        }

        // En recargas (tipoFront = 1), el precio de catálogo es el monto final sin comisión al cliente
        Integer tipoFront = producto.getTipoFront() != null ? producto.getTipoFront() : 1;
        return new ValidacionMontoResult(montoRequest, BigDecimal.ZERO, montoRequest, tipoFront);
    }

    @Override
    public void validarReferencia(String telefonoReferencia, ProductoItem producto) {
        if (telefonoReferencia == null || !TELEFONO_PATTERN.matcher(telefonoReferencia.trim()).matches()) {
            throw new MontoInvalidoException("El número telefónico de la recarga debe contener exactamente 10 dígitos numéricos.");
        }
    }
}
