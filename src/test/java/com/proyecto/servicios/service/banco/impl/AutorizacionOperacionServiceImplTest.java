package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.NoAutenticadoException;
import com.proyecto.servicios.exception.banco.*;
import com.proyecto.servicios.repository.banco.CuentaRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutorizacionOperacionServiceImpl")
class AutorizacionOperacionServiceImplTest {

    private static final String CORREO = "titular@banco.com";
    private static final String NUMERO_CUENTA = "01200000000000000016";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AutorizacionOperacionServiceImpl servicio;

    private Cliente cliente(long id, boolean activo) {
        return Cliente.builder().id(id).activo(activo).build();
    }

    private Usuario usuario(Cliente cliente, boolean activo) {
        return Usuario.builder().id(10L).correo(CORREO).cliente(cliente).activo(activo).password("hashUserPass").build();
    }

    private Cuenta cuenta(Cliente titular, EstatusCuenta estatus) {
        return Cuenta.builder()
                .id(100L)
                .numeroCuenta(NUMERO_CUENTA)
                .cliente(titular)
                .estatus(estatus)
                .pinHash("$2a$10$hashedPin")
                .intentosFallidosPin(0)
                .build();
    }

    @Test
    @DisplayName("Devuelve la cuenta cuando el solicitante es el titular y todo está activo")
    void titularActivo_devuelveCuenta() {
        Cliente titular = cliente(1L, true);
        Cuenta cuenta = cuenta(titular, EstatusCuenta.ACTIVA);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(titular, true)));
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));

        assertSame(cuenta, servicio.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO));
    }

    @Test
    @DisplayName("Cuenta de OTRO cliente → 404 (no puede debitar cuentas ajenas)")
    void cuentaAjena_noEncontrada() {
        Cliente solicitante = cliente(1L, true);
        Cliente otroTitular = cliente(2L, true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(solicitante, true)));
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA))
                .thenReturn(Optional.of(cuenta(otroTitular, EstatusCuenta.ACTIVA)));

        CuentaNoEncontradaException ex = assertThrows(CuentaNoEncontradaException.class,
                () -> servicio.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO));
        assertEquals(AutorizacionOperacionServiceImpl.CUENTA_NO_ENCONTRADA, ex.getMessage());
    }

    @Test
    @DisplayName("Cuenta inexistente responde IGUAL que una ajena (evita enumerar cuentas)")
    void cuentaInexistente_mismoMensajeQueAjena() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(cliente(1L, true), true)));
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.empty());

        CuentaNoEncontradaException ex = assertThrows(CuentaNoEncontradaException.class,
                () -> servicio.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO));
        assertEquals(AutorizacionOperacionServiceImpl.CUENTA_NO_ENCONTRADA, ex.getMessage());
    }

    @Test
    @DisplayName("Identidad que no existe en BD → no autenticado")
    void usuarioInexistente_noAutenticado() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        assertThrows(NoAutenticadoException.class,
                () -> servicio.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO));
        verifyNoInteractions(cuentaRepository);
    }

    @Test
    @DisplayName("Usuario inactivo → acceso denegado sin consultar la cuenta")
    void usuarioInactivo_denegado() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(cliente(1L, true), false)));

        assertThrows(UsuarioInactivoException.class,
                () -> servicio.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO));
        verifyNoInteractions(cuentaRepository);
    }

    @Test
    @DisplayName("Cliente dado de baja → acceso denegado")
    void clienteInactivo_denegado() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(cliente(1L, false), true)));

        assertThrows(UsuarioInactivoException.class,
                () -> servicio.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO));
        verifyNoInteractions(cuentaRepository);
    }

    @Test
    @DisplayName("Cuenta bloqueada o inactiva → acceso denegado")
    void cuentaBloqueada_denegada() {
        Cliente titular = cliente(1L, true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(titular, true)));
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA))
                .thenReturn(Optional.of(cuenta(titular, EstatusCuenta.BLOQUEADA)));

        assertThrows(CuentaBloqueadaException.class,
                () -> servicio.obtenerCuentaAutorizada(NUMERO_CUENTA, CORREO));
    }

    // --- TESTS DE PIN TRANSACCIONAL ---

    @Test
    @DisplayName("PIN correcto → limpia intentos previos y no lanza excepción")
    void pinCorrecto_validaSinExcepcion() {
        Cuenta c = cuenta(cliente(1L, true), EstatusCuenta.ACTIVA);
        c.setIntentosFallidosPin(1);
        when(passwordEncoder.matches("1234", "$2a$10$hashedPin")).thenReturn(true);

        assertDoesNotThrow(() -> servicio.validarPinTransaccional(c, "1234"));
        assertEquals(0, c.getIntentosFallidosPin());
        verify(cuentaRepository).save(c);
    }

    @Test
    @DisplayName("PIN incorrecto (1er intento) → incrementa a 1 y lanza PinInvalidoException")
    void pinIncorrecto_primerIntento_incrementa() {
        Cuenta c = cuenta(cliente(1L, true), EstatusCuenta.ACTIVA);
        when(passwordEncoder.matches("9999", "$2a$10$hashedPin")).thenReturn(false);

        PinInvalidoException ex = assertThrows(PinInvalidoException.class,
                () -> servicio.validarPinTransaccional(c, "9999"));
        assertTrue(ex.getMessage().contains("Intentos restantes antes de bloqueo: 2"));
        assertEquals(1, c.getIntentosFallidosPin());
        assertNull(c.getBloqueoPinHasta());
        verify(cuentaRepository).save(c);
    }

    @Test
    @DisplayName("PIN incorrecto (3er intento) → bloquea cuenta por 15 minutos")
    void pinIncorrecto_tercerIntento_bloqueaCuenta() {
        Cuenta c = cuenta(cliente(1L, true), EstatusCuenta.ACTIVA);
        c.setIntentosFallidosPin(2);
        when(passwordEncoder.matches("9999", "$2a$10$hashedPin")).thenReturn(false);

        PinBloqueadoException ex = assertThrows(PinBloqueadoException.class,
                () -> servicio.validarPinTransaccional(c, "9999"));
        assertTrue(ex.getMessage().contains("bloqueado por 15 minutos"));
        assertEquals(3, c.getIntentosFallidosPin());
        assertNotNull(c.getBloqueoPinHasta());
        verify(cuentaRepository).save(c);
    }

    @Test
    @DisplayName("Cuenta con bloqueo activo → rechaza inmediatamente sin checar PIN")
    void cuentaConBloqueoActivo_rechazaInmediatamente() {
        Cuenta c = cuenta(cliente(1L, true), EstatusCuenta.ACTIVA);
        c.setBloqueoPinHasta(OffsetDateTime.now().plusMinutes(10));

        assertThrows(PinBloqueadoException.class,
                () -> servicio.validarPinTransaccional(c, "1234"));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Cuenta sin pinHash (fallback contraseña usuario): 3er intento fallido también bloquea por 15 min")
    void cuentaSinPinHash_tercerIntentoFallido_bloqueaCuenta() {
        Cliente titular = cliente(1L, true);
        Cuenta c = cuenta(titular, EstatusCuenta.ACTIVA);
        c.setPinHash(null);
        c.setIntentosFallidosPin(2);

        when(usuarioRepository.findByClienteId(1L)).thenReturn(Optional.of(usuario(titular, true)));
        when(passwordEncoder.matches("WrongPass!", "hashUserPass")).thenReturn(false);

        PinBloqueadoException ex = assertThrows(PinBloqueadoException.class,
                () -> servicio.validarPinTransaccional(c, "WrongPass!"));
        assertTrue(ex.getMessage().contains("bloqueado por 15 minutos"));
        assertEquals(3, c.getIntentosFallidosPin());
        assertNotNull(c.getBloqueoPinHasta());
        verify(cuentaRepository).save(c);
    }
}
