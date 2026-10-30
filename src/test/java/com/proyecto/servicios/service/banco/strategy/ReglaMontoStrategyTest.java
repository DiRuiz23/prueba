package com.proyecto.servicios.service.banco.strategy;

import com.proyecto.servicios.client.model.CatProductXml.ProductoItem;
import com.proyecto.servicios.exception.banco.MontoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Estrategias de Validación de Monto y Referencia")
class ReglaMontoStrategyTest {

    private final RecargaStrategy recargaStrategy = new RecargaStrategy();
    private final PagoServicioStrategy pagoServicioStrategy = new PagoServicioStrategy(new BigDecimal("5000.00"));

    @Test
    @DisplayName("Recarga (tipoFront=1): monto exacto al catálogo es aceptado con comisión cero")
    void recarga_montoExacto_esAceptado() {
        ProductoItem item = new ProductoItem();
        item.setTipoFront(1);
        item.setPrecio("50.00");

        ValidacionMontoResult result = recargaStrategy.validarYCalcular(new BigDecimal("50.00"), item);

        assertEquals(new BigDecimal("50.00"), result.montoOperacion());
        assertEquals(BigDecimal.ZERO, result.comision());
        assertEquals(new BigDecimal("50.00"), result.montoDebitadoTotal());
        assertEquals(1, result.tipoFront());
    }

    @Test
    @DisplayName("Recarga (tipoFront=1): monto manipulado/distinto al catálogo es RECHAZADO")
    void recarga_montoManipulado_esRechazado() {
        ProductoItem item = new ProductoItem();
        item.setTipoFront(1);
        item.setPrecio("50.00");

        MontoInvalidoException ex = assertThrows(MontoInvalidoException.class,
                () -> recargaStrategy.validarYCalcular(new BigDecimal("100.00"), item));
        assertTrue(ex.getMessage().contains("no coincide con el valor exacto de la recarga en catálogo"));
    }

    @Test
    @DisplayName("Recarga (tipoFront=1): valida teléfono de 10 dígitos numéricos")
    void recarga_validaTelefono10Digitos() {
        ProductoItem item = new ProductoItem();
        item.setTipoFront(1);

        assertDoesNotThrow(() -> recargaStrategy.validarReferencia("5512345678", item));

        assertThrows(MontoInvalidoException.class,
                () -> recargaStrategy.validarReferencia("55123", item));
        assertThrows(MontoInvalidoException.class,
                () -> recargaStrategy.validarReferencia("551234567890", item));
        assertThrows(MontoInvalidoException.class,
                () -> recargaStrategy.validarReferencia("55ABCD5678", item));
    }

    @Test
    @DisplayName("Pago de Servicio (tipoFront=2): suma la comisión de catálogo al monto solicitado")
    void pagoServicio_sumaComisionAlDebito() {
        ProductoItem item = new ProductoItem();
        item.setTipoFront(2);
        item.setPrecio("12.00"); // comisión en catálogo

        ValidacionMontoResult result = pagoServicioStrategy.validarYCalcular(new BigDecimal("150.00"), item);

        assertEquals(new BigDecimal("150.00"), result.montoOperacion());
        assertEquals(new BigDecimal("12.00"), result.comision());
        assertEquals(new BigDecimal("162.00"), result.montoDebitadoTotal());
        assertEquals(2, result.tipoFront());
    }

    @Test
    @DisplayName("Pago de Servicio (tipoFront=2): monto negativo o cero es rechazado")
    void pagoServicio_montoCeroONegativo_rechazado() {
        ProductoItem item = new ProductoItem();
        item.setTipoFront(2);
        item.setPrecio("12.00");

        assertThrows(MontoInvalidoException.class,
                () -> pagoServicioStrategy.validarYCalcular(BigDecimal.ZERO, item));
        assertThrows(MontoInvalidoException.class,
                () -> pagoServicioStrategy.validarYCalcular(new BigDecimal("-10.00"), item));
    }

    @Test
    @DisplayName("Pago de Servicio (tipoFront=2): monto que supera el límite de transacción es rechazado")
    void pagoServicio_superaLimite_rechazado() {
        ProductoItem item = new ProductoItem();
        item.setTipoFront(2);
        item.setPrecio("12.00");

        MontoInvalidoException ex = assertThrows(MontoInvalidoException.class,
                () -> pagoServicioStrategy.validarYCalcular(new BigDecimal("5000.01"), item));
        assertTrue(ex.getMessage().contains("supera el límite máximo permitido"));
    }
}
