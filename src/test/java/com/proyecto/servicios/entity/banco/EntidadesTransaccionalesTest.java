package com.proyecto.servicios.entity.banco;

import com.proyecto.servicios.enums.EstatusTransaccionServicio;
import com.proyecto.servicios.enums.TipoMovimientoCuenta;
import com.proyecto.servicios.enums.TipoOperacionBitacora;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Entidades Transaccionales y de Auditoría (Fase 1)")
class EntidadesTransaccionalesTest {

    @Test
    @DisplayName("Cuenta mapea pinHash, intentos de pin y bloqueo con valores por defecto")
    void cuenta_mapeaCamposPinYBloqueo() {
        Cuenta cuenta = Cuenta.builder()
                .id(1L)
                .numeroCuenta("01200000000000000001")
                .pinHash("$2a$10$hashedpinvalue")
                .build();

        assertEquals("$2a$10$hashedpinvalue", cuenta.getPinHash());
        assertEquals("$2a$10$hashedpinvalue", cuenta.getPin());
        assertEquals(0, cuenta.getIntentosFallidosPin());
        assertNull(cuenta.getBloqueoPinHasta());

        OffsetDateTime bloqueo = OffsetDateTime.now().plusMinutes(15);
        cuenta.setIntentosFallidosPin(3);
        cuenta.setBloqueoPinHasta(bloqueo);

        assertEquals(3, cuenta.getIntentosFallidosPin());
        assertEquals(bloqueo, cuenta.getBloqueoPinHasta());
    }

    @Test
    @DisplayName("TransaccionServicio mapea campos de idempotencia, conciliación y versión optimista")
    void transaccionServicio_mapeaCamposCompletos() {
        Cuenta cuenta = Cuenta.builder().id(10L).build();
        Usuario usuario = Usuario.builder().id(5L).build();

        TransaccionServicio tx = TransaccionServicio.builder()
                .cuenta(cuenta)
                .usuario(usuario)
                .idempotencyKey("550e8400-e29b-41d4-a716-446655440000")
                .requestHash("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
                .horaLocal("20261027140000")
                .payloadHash("payloadhash123")
                .upc("GP000000000001")
                .idProducto(101)
                .idServicio(202)
                .telefonoReferencia("5512345678")
                .monto(new BigDecimal("100.00"))
                .comision(new BigDecimal("15.00"))
                .montoDebitado(new BigDecimal("115.00"))
                .saldoAnterior(new BigDecimal("500.00"))
                .saldoPosterior(new BigDecimal("385.00"))
                .estatus(EstatusTransaccionServicio.EN_CONCILIACION)
                .intentosConfirmacion(1)
                .proximaConfirmacionAt(OffsetDateTime.now().plusSeconds(62))
                .fechaLimiteConciliacion(OffsetDateTime.now().plusHours(24))
                .build();

        assertEquals("550e8400-e29b-41d4-a716-446655440000", tx.getIdempotencyKey());
        assertEquals(EstatusTransaccionServicio.EN_CONCILIACION, tx.getEstatus());
        assertEquals(new BigDecimal("15.00"), tx.getComision());
        assertEquals(new BigDecimal("115.00"), tx.getMontoDebitado());
        assertEquals(0L, tx.getVersion());
        assertEquals(1, tx.getIntentosConfirmacion());
        assertNotNull(tx.getProximaConfirmacionAt());
        assertNotNull(tx.getFechaLimiteConciliacion());
    }

    @Test
    @DisplayName("MovimientoCuenta registra libro contable inmutable con tipo de movimiento y saldos")
    void movimientoCuenta_registraMovimientoCorrectamente() {
        Cuenta cuenta = Cuenta.builder().id(10L).build();
        TransaccionServicio tx = TransaccionServicio.builder().id(99L).build();

        MovimientoCuenta movimiento = MovimientoCuenta.builder()
                .cuenta(cuenta)
                .transaccionServicio(tx)
                .tipo(TipoMovimientoCuenta.DEBITO_PAGO)
                .monto(new BigDecimal("115.00"))
                .saldoAnterior(new BigDecimal("500.00"))
                .saldoPosterior(new BigDecimal("385.00"))
                .build();

        assertSame(cuenta, movimiento.getCuenta());
        assertSame(tx, movimiento.getTransaccionServicio());
        assertEquals(TipoMovimientoCuenta.DEBITO_PAGO, movimiento.getTipo());
        assertEquals(new BigDecimal("115.00"), movimiento.getMonto());
        assertEquals(new BigDecimal("500.00"), movimiento.getSaldoAnterior());
        assertEquals(new BigDecimal("385.00"), movimiento.getSaldoPosterior());
    }

    @Test
    @DisplayName("GestoPagoBitacora registra petición/respuesta técnica con enmascarado y códigos HTTP")
    void gestopagoBitacora_registraAuditoriaTecnica() {
        TransaccionServicio tx = TransaccionServicio.builder().id(99L).build();

        GestoPagoBitacora bitacora = GestoPagoBitacora.builder()
                .transaccionServicio(tx)
                .upc("GP000000000001")
                .tipoOperacion(TipoOperacionBitacora.SEND_TX)
                .requestEnmascarado("<xml><telefono>******5678</telefono></xml>")
                .responseEnmascarado("<xml><codigo>01</codigo></xml>")
                .codigoHttp(200)
                .codigoRespuestaProveedor("01")
                .duracionMs(1250L)
                .build();

        assertEquals(TipoOperacionBitacora.SEND_TX, bitacora.getTipoOperacion());
        assertEquals(200, bitacora.getCodigoHttp());
        assertEquals("01", bitacora.getCodigoRespuestaProveedor());
        assertEquals(1250L, bitacora.getDuracionMs());
        assertTrue(bitacora.getRequestEnmascarado().contains("******5678"));
    }
}
