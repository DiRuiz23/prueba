package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.dto.request.banco.*;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.CuentaResponse;
import com.proyecto.servicios.dto.response.banco.SaldoResponse;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.banco.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.banco.CuentaNoEncontradaException;
import com.proyecto.servicios.repository.banco.ClienteRepository;
import com.proyecto.servicios.repository.banco.CuentaRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: CuentaServiceImpl")
class CuentaServiceImplTest {

    private static final String NUMERO_CUENTA = "012123456789";

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CuentaServiceImpl cuentaService;

    private Cliente cliente(Long id, boolean activo) {
        return Cliente.builder()
                .id(id)
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .activo(activo)
                .build();
    }

    private Cuenta cuenta(Long id, Cliente cliente, String numero, BigDecimal saldo, EstatusCuenta estatus) {
        return Cuenta.builder()
                .id(id)
                .cliente(cliente)
                .numeroCuenta(numero)
                .saldo(saldo)
                .estatus(estatus)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSULTAS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("consultarPorNumeroCuenta: cuenta encontrada exitosamente")
    void consultarPorNumeroCuenta_exitosa() {
        Cliente cl = cliente(1L, true);
        Cuenta c = cuenta(10L, cl, NUMERO_CUENTA, new BigDecimal("1500.00"), EstatusCuenta.ACTIVA);

        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(c));

        NumeroCuentaRequest request = new NumeroCuentaRequest();
        request.setNumeroCuenta("  " + NUMERO_CUENTA + "  ");

        CuentaResponse response = cuentaService.consultarPorNumeroCuenta(request);

        assertNotNull(response);
        assertEquals(NUMERO_CUENTA, response.getNumeroCuenta());
        assertEquals(new BigDecimal("1500.00"), response.getSaldo());
    }

    @Test
    @DisplayName("consultarPorNumeroCuenta: cuenta no existe lanza CuentaNoEncontradaException")
    void consultarPorNumeroCuenta_noExiste() {
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.empty());

        NumeroCuentaRequest request = new NumeroCuentaRequest();
        request.setNumeroCuenta(NUMERO_CUENTA);

        assertThrows(CuentaNoEncontradaException.class, () -> cuentaService.consultarPorNumeroCuenta(request));
    }

    @Test
    @DisplayName("consultar: retorna página con resultados")
    void consultar_conFiltros() {
        Cliente cl = cliente(1L, true);
        Cuenta c = cuenta(10L, cl, NUMERO_CUENTA, new BigDecimal("1000.00"), EstatusCuenta.ACTIVA);
        Page<Cuenta> page = new PageImpl<>(List.of(c));

        when(cuentaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        CuentaFiltroRequest request = new CuentaFiltroRequest();
        request.setNumeroCuenta(NUMERO_CUENTA);
        request.setClienteId(1L);

        PaginaResponse<CuentaResponse> result = cuentaService.consultar(request);

        assertNotNull(result);
        assertEquals(1, result.getContenido().size());
        assertEquals(NUMERO_CUENTA, result.getContenido().get(0).getNumeroCuenta());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("crearCuentaAdicional: cliente activo con saldo inicial default (cero)")
    void crearCuentaAdicional_saldoDefault() {
        Cliente cl = cliente(1L, true);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cl));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearCuentaRequest request = new CrearCuentaRequest();
        request.setClienteId(1L);
        request.setSaldoInicial(null);

        CuentaResponse response = cuentaService.crearCuentaAdicional(request);

        assertNotNull(response);
        assertTrue(response.getNumeroCuenta().startsWith("012"));
        assertEquals(BigDecimal.ZERO, response.getSaldo());
        assertEquals(EstatusCuenta.ACTIVA, response.getEstatus());
        verify(cuentaRepository).save(any(Cuenta.class));
    }

    @Test
    @DisplayName("crearCuentaAdicional: cliente activo con saldo inicial especificado")
    void crearCuentaAdicional_conSaldo() {
        Cliente cl = cliente(1L, true);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cl));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearCuentaRequest request = new CrearCuentaRequest();
        request.setClienteId(1L);
        request.setSaldoInicial(new BigDecimal("500.00"));

        CuentaResponse response = cuentaService.crearCuentaAdicional(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("500.00"), response.getSaldo());
    }

    @Test
    @DisplayName("crearCuentaAdicional: cliente no encontrado lanza ClienteNoEncontradoException")
    void crearCuentaAdicional_clienteNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        CrearCuentaRequest request = new CrearCuentaRequest();
        request.setClienteId(99L);

        assertThrows(ClienteNoEncontradoException.class, () -> cuentaService.crearCuentaAdicional(request));
    }

    @Test
    @DisplayName("crearCuentaAdicional: cliente inactivo lanza ReglaNegocioException")
    void crearCuentaAdicional_clienteInactivo() {
        Cliente inactivo = cliente(2L, false);
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(inactivo));

        CrearCuentaRequest request = new CrearCuentaRequest();
        request.setClienteId(2L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> cuentaService.crearCuentaAdicional(request));
        assertTrue(ex.getMessage().contains("cliente dado de baja"));
    }

    @Test
    @DisplayName("crearCuentaAdicional: saldo inicial negativo lanza ReglaNegocioException")
    void crearCuentaAdicional_saldoNegativo() {
        Cliente cl = cliente(1L, true);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cl));

        CrearCuentaRequest request = new CrearCuentaRequest();
        request.setClienteId(1L);
        request.setSaldoInicial(new BigDecimal("-100.00"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> cuentaService.crearCuentaAdicional(request));
        assertTrue(ex.getMessage().contains("no puede ser negativo"));
    }

    @Test
    @DisplayName("crearCuentaAdicional: colisión de número de cuenta reintenta generación")
    void crearCuentaAdicional_colisionNumeroCuenta() {
        Cliente cl = cliente(1L, true);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cl));
        // Primera vez colisiona, segunda vez es único
        when(cuentaRepository.existsByNumeroCuenta(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearCuentaRequest request = new CrearCuentaRequest();
        request.setClienteId(1L);

        CuentaResponse response = cuentaService.crearCuentaAdicional(request);

        assertNotNull(response);
        verify(cuentaRepository, times(2)).existsByNumeroCuenta(anyString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTUALIZACIÓN DE ESTATUS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cambiarEstatusCuenta: cambio exitoso a BLOQUEADA")
    void cambiarEstatusCuenta_exitoso() {
        Cliente cl = cliente(1L, true);
        Cuenta c = cuenta(10L, cl, NUMERO_CUENTA, new BigDecimal("1000.00"), EstatusCuenta.ACTIVA);

        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(c));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarEstatusCuentaRequest request = new ActualizarEstatusCuentaRequest();
        request.setNumeroCuenta(NUMERO_CUENTA);
        request.setNuevoEstatus(EstatusCuenta.BLOQUEADA);

        CuentaResponse response = cuentaService.cambiarEstatusCuenta(request);

        assertNotNull(response);
        assertEquals(EstatusCuenta.BLOQUEADA, response.getEstatus());
    }

    @Test
    @DisplayName("cambiarEstatusCuenta: cuenta no encontrada lanza CuentaNoEncontradaException")
    void cambiarEstatusCuenta_noExiste() {
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.empty());

        ActualizarEstatusCuentaRequest request = new ActualizarEstatusCuentaRequest();
        request.setNumeroCuenta(NUMERO_CUENTA);
        request.setNuevoEstatus(EstatusCuenta.BLOQUEADA);

        assertThrows(CuentaNoEncontradaException.class, () -> cuentaService.cambiarEstatusCuenta(request));
    }

    @Test
    @DisplayName("cambiarEstatusCuenta: activar cuenta de cliente inactivo lanza ReglaNegocioException")
    void cambiarEstatusCuenta_activarConClienteInactivo() {
        Cliente inactivo = cliente(1L, false);
        Cuenta c = cuenta(10L, inactivo, NUMERO_CUENTA, new BigDecimal("1000.00"), EstatusCuenta.INACTIVA);

        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(c));

        ActualizarEstatusCuentaRequest request = new ActualizarEstatusCuentaRequest();
        request.setNumeroCuenta(NUMERO_CUENTA);
        request.setNuevoEstatus(EstatusCuenta.ACTIVA);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> cuentaService.cambiarEstatusCuenta(request));
        assertTrue(ex.getMessage().contains("perteneciente a un cliente inactivo"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SALDO
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("consultarSaldo: consulta exitosa de saldo")
    void consultarSaldo_exitosa() {
        Cliente cl = cliente(1L, true);
        Cuenta c = cuenta(10L, cl, NUMERO_CUENTA, new BigDecimal("2500.50"), EstatusCuenta.ACTIVA);

        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(c));

        NumeroCuentaRequest request = new NumeroCuentaRequest();
        request.setNumeroCuenta(NUMERO_CUENTA);

        SaldoResponse response = cuentaService.consultarSaldo(request);

        assertNotNull(response);
        assertEquals(NUMERO_CUENTA, response.getNumeroCuenta());
        assertEquals(new BigDecimal("2500.50"), response.getSaldo());
    }

    @Test
    @DisplayName("consultarSaldo: cuenta inexistente lanza CuentaNoEncontradaException")
    void consultarSaldo_noExiste() {
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.empty());

        NumeroCuentaRequest request = new NumeroCuentaRequest();
        request.setNumeroCuenta(NUMERO_CUENTA);

        assertThrows(CuentaNoEncontradaException.class, () -> cuentaService.consultarSaldo(request));
    }

    @Test
    @DisplayName("obtenerMisCuentas: retorna cuentas del usuario autenticado")
    void obtenerMisCuentas_exitoso() {
        Cliente cl = cliente(1L, true);
        Usuario u = Usuario.builder().id(5L).correo("cliente@banco.com").cliente(cl).activo(true).build();
        Cuenta c = cuenta(10L, cl, NUMERO_CUENTA, new BigDecimal("1000.00"), EstatusCuenta.ACTIVA);

        when(usuarioRepository.findByCorreo("cliente@banco.com")).thenReturn(Optional.of(u));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(c));

        List<CuentaResponse> res = cuentaService.obtenerMisCuentas("  CLIENTE@banco.com ");

        assertEquals(1, res.size());
        assertEquals(NUMERO_CUENTA, res.get(0).getNumeroCuenta());
        assertEquals(new BigDecimal("1000.00"), res.get(0).getSaldo());
    }
}

