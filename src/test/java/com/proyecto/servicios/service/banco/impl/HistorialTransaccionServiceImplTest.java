package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.dto.response.banco.PagoServicioResponse;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.enums.EstatusTransaccionServicio;
import com.proyecto.servicios.exception.NoAutenticadoException;
import com.proyecto.servicios.repository.banco.TransaccionServicioRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: HistorialTransaccionServiceImpl (Historial exclusivo de operaciones exitosas)")
class HistorialTransaccionServiceImplTest {

    @Mock
    private TransaccionServicioRepository transaccionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private HistorialTransaccionServiceImpl historialService;

    private Usuario usuario;
    private Cliente cliente;
    private Cuenta cuenta;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder().id(100L).nombre("Juan").build();
        cuenta = Cuenta.builder().id(200L).numeroCuenta("0123456789").cliente(cliente).build();
        usuario = Usuario.builder().id(10L).correo("juan@banco.com").cliente(cliente).activo(true).build();
    }

    @Test
    @DisplayName("obtenerHistorialExitoso: retorna compras exitosas paginadas del cliente")
    void obtenerHistorialExitoso_conCliente_exitoso() {
        Pageable pageable = PageRequest.of(0, 5);

        TransaccionServicio tx = TransaccionServicio.builder()
                .id(1L)
                .upc("GP123456")
                .idTxGestopago("TX-999")
                .numeroAutorizacion("AUTH-111")
                .cuenta(cuenta)
                .monto(new BigDecimal("150.00"))
                .montoDebitado(new BigDecimal("150.00"))
                .saldoPosterior(new BigDecimal("850.00"))
                .telefonoReferencia("5512345678")
                .idProducto(101)
                .idServicio(50)
                .estatus(EstatusTransaccionServicio.EXITOSA)
                .fechaTransaccion(OffsetDateTime.now())
                .build();

        Page<TransaccionServicio> pagina = new PageImpl<>(List.of(tx), pageable, 1);

        when(usuarioRepository.findByCorreo("juan@banco.com")).thenReturn(Optional.of(usuario));
        when(transaccionRepository.findByCuentaClienteIdAndEstatus(100L, EstatusTransaccionServicio.EXITOSA, pageable))
                .thenReturn(pagina);

        Page<PagoServicioResponse> resultado = historialService.obtenerHistorialExitoso("juan@banco.com", pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        PagoServicioResponse item = resultado.getContent().get(0);
        assertEquals(1L, item.getIdTransaccion());
        assertEquals("GP123456", item.getUpc());
        assertEquals("TX-999", item.getIdTxGestoPago());
        assertEquals("EXITOSA", item.getEstatus());
        assertEquals(new BigDecimal("150.00"), item.getMontoDebitado());
    }

    @Test
    @DisplayName("obtenerHistorialExitoso: usuario sin cliente consulta por usuarioId")
    void obtenerHistorialExitoso_sinCliente() {
        usuario.setCliente(null);
        Pageable pageable = PageRequest.of(0, 5);
        Page<TransaccionServicio> paginaVacia = new PageImpl<>(List.of(), pageable, 0);

        when(usuarioRepository.findByCorreo("juan@banco.com")).thenReturn(Optional.of(usuario));
        when(transaccionRepository.findByUsuarioIdAndEstatus(10L, EstatusTransaccionServicio.EXITOSA, pageable))
                .thenReturn(paginaVacia);

        Page<PagoServicioResponse> resultado = historialService.obtenerHistorialExitoso("juan@banco.com", pageable);

        assertNotNull(resultado);
        assertEquals(0, resultado.getTotalElements());
        verify(transaccionRepository).findByUsuarioIdAndEstatus(10L, EstatusTransaccionServicio.EXITOSA, pageable);
    }

    @Test
    @DisplayName("obtenerHistorialExitoso: usuario no encontrado lanza NoAutenticadoException")
    void obtenerHistorialExitoso_usuarioNoEncontrado() {
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.findByCorreo("fantasma@banco.com")).thenReturn(Optional.empty());

        assertThrows(NoAutenticadoException.class,
                () -> historialService.obtenerHistorialExitoso("fantasma@banco.com", pageable));
    }
}

