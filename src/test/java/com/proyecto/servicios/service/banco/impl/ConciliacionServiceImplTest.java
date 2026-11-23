package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.client.model.GestoPagoSendTxPayload;
import com.proyecto.servicios.client.model.GestoPagoTxResponseXml;
import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.enums.EstatusTransaccionServicio;
import com.proyecto.servicios.repository.banco.TransaccionServicioRepository;
import com.proyecto.servicios.service.banco.CuentaDebitoService;
import com.proyecto.servicios.service.gestopago.GestoPagoTxGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: ConciliacionServiceImpl")
class ConciliacionServiceImplTest {

    @Mock
    private TransaccionServicioRepository transaccionRepository;

    @Mock
    private GestoPagoTxGateway gestoPagoTxGateway;

    @Mock
    private CuentaDebitoService cuentaDebitoService;

    @InjectMocks
    private ConciliacionServiceImpl conciliacionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(conciliacionService, "unidad", "083");
    }

    private TransaccionServicio crearTx(Long id, Integer tipoFront) {
        return TransaccionServicio.builder()
                .id(id)
                .upc("GP0000000001")
                .idProducto(101)
                .idServicio(50)
                .tipoFront(tipoFront)
                .telefonoReferencia("5512345678")
                .monto(new BigDecimal("100.00"))
                .horaLocal("20261109120000")
                .estatus(EstatusTransaccionServicio.EN_CONCILIACION)
                .intentosConfirmacion(0)
                .fechaLimiteConciliacion(OffsetDateTime.now(ZoneId.systemDefault()).plusHours(12))
                .build();
    }

    @Test
    @DisplayName("procesarTransaccionesEnConciliacion: sin transacciones candidatas retorna 0")
    void procesar_sinCandidatas() {
        when(cuentaDebitoService.reclamarCandidatasConciliacion(any(OffsetDateTime.class), eq(20)))
                .thenReturn(Collections.emptyList());

        int procesadas = conciliacionService.procesarTransaccionesEnConciliacion();

        assertEquals(0, procesadas);
        verifyNoInteractions(gestoPagoTxGateway);
    }

    @Test
    @DisplayName("procesarTransaccionesEnConciliacion: transacción que supera 24 horas pasa a REVISION_MANUAL")
    void procesar_superaLimite24Horas() {
        TransaccionServicio tx = crearTx(1L, 1);
        tx.setFechaLimiteConciliacion(OffsetDateTime.now(ZoneId.systemDefault()).minusHours(1)); // Vencida

        when(cuentaDebitoService.reclamarCandidatasConciliacion(any(), eq(20)))
                .thenReturn(List.of(tx));

        int procesadas = conciliacionService.procesarTransaccionesEnConciliacion();

        assertEquals(1, procesadas);
        assertEquals(EstatusTransaccionServicio.REVISION_MANUAL, tx.getEstatus());
        assertTrue(tx.getMensajeRespuesta().contains("Superado límite de 24 horas"));
        verify(transaccionRepository).save(tx);
        verifyNoInteractions(gestoPagoTxGateway);
    }

    @Test
    @DisplayName("procesarTransaccionesEnConciliacion: confirmTx retorna 06 (APLICADA) → confirmarExito")
    void procesar_codigo06_confirmaExito() {
        TransaccionServicio tx = crearTx(1L, 1);
        when(cuentaDebitoService.reclamarCandidatasConciliacion(any(), eq(20)))
                .thenReturn(List.of(tx));

        GestoPagoTxResponseXml resp = new GestoPagoTxResponseXml();
        resp.setCodigo("06");
        resp.setTexto("Transacción aplicada con anterioridad");

        when(gestoPagoTxGateway.consultarTransaccion(any(GestoPagoSendTxPayload.class), eq(tx.getUpc()), eq(tx.getId())))
                .thenReturn(resp);

        int procesadas = conciliacionService.procesarTransaccionesEnConciliacion();

        assertEquals(1, procesadas);
        verify(cuentaDebitoService).confirmarExito(tx, resp);
    }

    @Test
    @DisplayName("procesarTransaccionesEnConciliacion: confirmTx retorna 70 (NO APLICADA) → reembolsar")
    void procesar_codigo70_reembolsa() {
        TransaccionServicio tx = crearTx(1L, 2);
        when(cuentaDebitoService.reclamarCandidatasConciliacion(any(), eq(20)))
                .thenReturn(List.of(tx));

        GestoPagoTxResponseXml resp = new GestoPagoTxResponseXml();
        resp.setCodigo("70");
        resp.setTexto("Transacción no aplicada");

        when(gestoPagoTxGateway.consultarTransaccion(any(GestoPagoSendTxPayload.class), eq(tx.getUpc()), eq(tx.getId())))
                .thenReturn(resp);

        int procesadas = conciliacionService.procesarTransaccionesEnConciliacion();

        assertEquals(1, procesadas);
        verify(cuentaDebitoService).ejecutarReembolsoConciliacion(eq(tx), contains("código 70"));
    }

    @Test
    @DisplayName("procesarTransaccionesEnConciliacion: confirmTx retorna 82 u otro código → reprograma con backoff")
    void procesar_codigo82_reprograma() {
        TransaccionServicio tx = crearTx(1L, 1);
        when(cuentaDebitoService.reclamarCandidatasConciliacion(any(), eq(20)))
                .thenReturn(List.of(tx));

        GestoPagoTxResponseXml resp = new GestoPagoTxResponseXml();
        resp.setCodigo("82");

        when(gestoPagoTxGateway.consultarTransaccion(any(), any(), any())).thenReturn(resp);

        int procesadas = conciliacionService.procesarTransaccionesEnConciliacion();

        assertEquals(1, procesadas);
        assertEquals(1, tx.getIntentosConfirmacion());
        assertNotNull(tx.getProximaConfirmacionAt());
        verify(transaccionRepository).save(tx);
    }

    @Test
    @DisplayName("procesarTransaccionesEnConciliacion: fallo en comunicación con gateway reprograma transacción")
    void procesar_errorGateway_reprograma() {
        TransaccionServicio tx = crearTx(1L, 1);
        when(cuentaDebitoService.reclamarCandidatasConciliacion(any(), eq(20)))
                .thenReturn(List.of(tx));

        when(gestoPagoTxGateway.consultarTransaccion(any(), any(), any()))
                .thenThrow(new RuntimeException("Timeout conexión"));

        int procesadas = conciliacionService.procesarTransaccionesEnConciliacion();

        assertEquals(1, procesadas);
        assertEquals(1, tx.getIntentosConfirmacion());
        verify(transaccionRepository).save(tx);
    }

    @Test
    @DisplayName("procesarTransaccionesEnConciliacion: construye payload con teléfono para recarga (tipoFront=1)")
    void procesar_armaPayloadTipoFront1() {
        TransaccionServicio tx = crearTx(1L, 1);
        when(cuentaDebitoService.reclamarCandidatasConciliacion(any(), eq(20)))
                .thenReturn(List.of(tx));

        GestoPagoTxResponseXml resp = new GestoPagoTxResponseXml();
        resp.setCodigo("06");
        when(gestoPagoTxGateway.consultarTransaccion(any(), any(), any())).thenReturn(resp);

        conciliacionService.procesarTransaccionesEnConciliacion();

        ArgumentCaptor<GestoPagoSendTxPayload> captor = ArgumentCaptor.forClass(GestoPagoSendTxPayload.class);
        verify(gestoPagoTxGateway).consultarTransaccion(captor.capture(), eq(tx.getUpc()), eq(tx.getId()));

        GestoPagoSendTxPayload payload = captor.getValue();
        assertEquals("5512345678", payload.getTelefono());
        assertNull(payload.getReferencia());
    }
}

