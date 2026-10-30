package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.client.model.GestoPagoTxResponseXml;
import com.proyecto.servicios.dto.request.banco.PagoServicioRequest;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.MovimientoCuenta;
import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.enums.EstatusTransaccionServicio;
import com.proyecto.servicios.enums.TipoMovimientoCuenta;
import com.proyecto.servicios.exception.banco.SaldoInsuficienteException;
import com.proyecto.servicios.repository.banco.CuentaRepository;
import com.proyecto.servicios.repository.banco.MovimientoCuentaRepository;
import com.proyecto.servicios.repository.banco.TransaccionServicioRepository;
import com.proyecto.servicios.service.banco.strategy.ValidacionMontoResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CuentaDebitoServiceImpl")
class CuentaDebitoServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private TransaccionServicioRepository transaccionRepository;
    @Mock
    private MovimientoCuentaRepository movimientoCuentaRepository;

    @InjectMocks
    private CuentaDebitoServiceImpl cuentaDebitoService;

    @Test
    @DisplayName("Reserva de fondos: débito atómico exitoso, genera Transacción PENDIENTE y Movimiento DEBITO_PAGO")
    void reservarFondos_exitoso() {
        Cuenta c = Cuenta.builder().id(1L).saldo(new BigDecimal("400.00")).build();
        when(cuentaRepository.debitarSaldoAtomico(1L, new BigDecimal("100.00"))).thenReturn(1);
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(c));
        when(transaccionRepository.save(any(TransaccionServicio.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PagoServicioRequest req = new PagoServicioRequest();
        req.setIdProducto(10);
        req.setIdServicio(20);
        req.setTelefonoReferencia("5512345678");

        ValidacionMontoResult montos = new ValidacionMontoResult(
                new BigDecimal("88.00"), new BigDecimal("12.00"), new BigDecimal("100.00"), 2
        );

        TransaccionServicio tx = cuentaDebitoService.reservarFondosYRegistrar(
                c, null, req, montos, "key-123", "hash-req", "GP0000000001", "20261028130000", "hash-payload"
        );

        assertNotNull(tx);
        assertEquals(EstatusTransaccionServicio.PENDIENTE, tx.getEstatus());
        assertEquals(new BigDecimal("100.00"), tx.getMontoDebitado());
        assertEquals(2, tx.getTipoFront());

        ArgumentCaptor<MovimientoCuenta> movCaptor = ArgumentCaptor.forClass(MovimientoCuenta.class);
        verify(movimientoCuentaRepository).save(movCaptor.capture());
        assertEquals(TipoMovimientoCuenta.DEBITO_PAGO, movCaptor.getValue().getTipo());
        assertEquals(new BigDecimal("100.00"), movCaptor.getValue().getMonto());
    }

    @Test
    @DisplayName("Reserva de fondos: saldo insuficiente (0 filas afectadas) → SaldoInsuficienteException sin guardar")
    void reservarFondos_saldoInsuficiente() {
        Cuenta c = Cuenta.builder().id(1L).saldo(new BigDecimal("50.00")).build();
        when(cuentaRepository.debitarSaldoAtomico(1L, new BigDecimal("100.00"))).thenReturn(0);

        ValidacionMontoResult montos = new ValidacionMontoResult(
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00")
        );

        assertThrows(SaldoInsuficienteException.class,
                () -> cuentaDebitoService.reservarFondosYRegistrar(
                        c, null, new PagoServicioRequest(), montos, "key-123", "hash-req", "GP0000000001", "hora", "payload"
                ));

        verifyNoInteractions(transaccionRepository);
        verifyNoInteractions(movimientoCuentaRepository);
    }

    @Test
    @DisplayName("Confirmar éxito: actualiza a EXITOSA y guarda respuesta del proveedor")
    void confirmarExito_actualizaTransaccion() {
        TransaccionServicio tx = TransaccionServicio.builder()
                .id(99L)
                .estatus(EstatusTransaccionServicio.PENDIENTE)
                .build();
        when(transaccionRepository.findById(99L)).thenReturn(Optional.of(tx));

        GestoPagoTxResponseXml xml = new GestoPagoTxResponseXml();
        xml.setIdTx("TX-8888");
        xml.setNumAutorizacion("AUTH-9999");
        xml.setCodigo("01");
        xml.setTexto("Aprobada");

        cuentaDebitoService.confirmarExito(tx, xml);

        assertEquals(EstatusTransaccionServicio.EXITOSA, tx.getEstatus());
        assertEquals("TX-8888", tx.getIdTxGestopago());
        assertEquals("AUTH-9999", tx.getNumeroAutorizacion());
        verify(transaccionRepository).save(tx);
    }

    @Test
    @DisplayName("Fallo definitivo: reembolsa saldo atómicamente, genera REVERSO_PAGO y marca FALLIDA")
    void registrarFallo_reembolsaYMarcaFallida() {
        Cuenta c = Cuenta.builder().id(1L).saldo(new BigDecimal("500.00")).build();
        TransaccionServicio tx = TransaccionServicio.builder()
                .id(99L)
                .cuenta(c)
                .montoDebitado(new BigDecimal("100.00"))
                .estatus(EstatusTransaccionServicio.PENDIENTE)
                .build();
        when(transaccionRepository.findById(99L)).thenReturn(Optional.of(tx));
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(c));

        cuentaDebitoService.registrarFalloYReembolsar(tx, null, "Error del distribuidor");

        assertEquals(EstatusTransaccionServicio.FALLIDA, tx.getEstatus());
        verify(cuentaRepository).reembolsarSaldoAtomico(1L, new BigDecimal("100.00"));

        ArgumentCaptor<MovimientoCuenta> movCaptor = ArgumentCaptor.forClass(MovimientoCuenta.class);
        verify(movimientoCuentaRepository).save(movCaptor.capture());
        assertEquals(TipoMovimientoCuenta.REVERSO_PAGO, movCaptor.getValue().getTipo());
        assertEquals(new BigDecimal("100.00"), movCaptor.getValue().getMonto());
    }

    @Test
    @DisplayName("Resultado incierto: actualiza a EN_CONCILIACION con proxima_confirmacion >= 62s")
    void registrarEnConciliacion_programaJob() {
        TransaccionServicio tx = TransaccionServicio.builder()
                .id(99L)
                .estatus(EstatusTransaccionServicio.PENDIENTE)
                .build();
        when(transaccionRepository.findById(99L)).thenReturn(Optional.of(tx));

        cuentaDebitoService.registrarEnConciliacion(tx, "82", "Timeout de comunicación");

        assertEquals(EstatusTransaccionServicio.EN_CONCILIACION, tx.getEstatus());
        assertNotNull(tx.getProximaConfirmacionAt());
        assertNotNull(tx.getFechaLimiteConciliacion());
        verify(transaccionRepository).save(tx);
        verifyNoInteractions(movimientoCuentaRepository);
    }

    @Test
    @DisplayName("Reclamar candidatas a conciliación: aplica lease de 90s bajo FOR UPDATE SKIP LOCKED")
    void reclamarCandidatasConciliacion_aplicaLeaseYGuarda() {
        java.time.OffsetDateTime ahora = java.time.OffsetDateTime.now();
        TransaccionServicio tx = TransaccionServicio.builder()
                .id(77L)
                .estatus(EstatusTransaccionServicio.EN_CONCILIACION)
                .proximaConfirmacionAt(ahora.minusSeconds(10))
                .build();

        when(transaccionRepository.findCandidatasConciliacionParaProcesar(ahora, 20))
                .thenReturn(java.util.List.of(tx));

        java.util.List<TransaccionServicio> reclamadas = cuentaDebitoService.reclamarCandidatasConciliacion(ahora, 20);

        assertEquals(1, reclamadas.size());
        assertEquals(ahora.plusSeconds(90), tx.getProximaConfirmacionAt());
        verify(transaccionRepository).saveAll(reclamadas);
    }
}

