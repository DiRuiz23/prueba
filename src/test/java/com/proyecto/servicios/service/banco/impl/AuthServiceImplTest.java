package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.config.security.JwtService;
import com.proyecto.servicios.dto.request.auth.LoginRequest;
import com.proyecto.servicios.dto.request.auth.VerificarOtpRequest;
import com.proyecto.servicios.dto.response.auth.LoginResponse;
import com.proyecto.servicios.entity.banco.SesionActiva;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.CredencialesInvalidasException;
import com.proyecto.servicios.exception.banco.UsuarioInactivoException;
import com.proyecto.servicios.exception.banco.UsuarioNoEncontradoException;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.service.banco.OtpService;
import com.proyecto.servicios.service.banco.SesionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: AuthServiceImpl (Flujo 2FA y Sesión Única)")
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private OtpService otpService;
    @Mock
    private SesionService sesionService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .correo("titular@banco.com")
                .password("$2a$10$hash")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("login: credenciales válidas y sin sesión previa solicita 2FA (Paso 1)")
    void login_exitoso_solicita2fa() {
        LoginRequest req = new LoginRequest("titular@banco.com", "Password123", "MOVIL", false);

        when(usuarioRepository.findByCorreo("titular@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123", "$2a$10$hash")).thenReturn(true);
        when(sesionService.obtenerSesionActivaPrevia(1L)).thenReturn(Optional.empty());
        when(otpService.generarYEnviarOtp(usuario, "LOGIN_2FA")).thenReturn(300);

        LoginResponse resp = authService.login(req);

        assertNotNull(resp);
        assertTrue(resp.getRequiere2fa());
        assertFalse(resp.getSesionActivaDetectada());
        assertEquals("titular@banco.com", resp.getCorreo());
        assertEquals(300, resp.getTiempoExpiracionOtpSegundos());
        verify(otpService).generarYEnviarOtp(usuario, "LOGIN_2FA");
    }

    @Test
    @DisplayName("login: detecta sesión activa previa en otro dispositivo y advierte al usuario")
    void login_sesionActivaPrevia_advierte() {
        LoginRequest req = new LoginRequest("titular@banco.com", "Password123", "WEB", false);
        SesionActiva sesionPrevia = SesionActiva.builder().id(99L).dispositivo("MOVIL").build();

        when(usuarioRepository.findByCorreo("titular@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123", "$2a$10$hash")).thenReturn(true);
        when(sesionService.obtenerSesionActivaPrevia(1L)).thenReturn(Optional.of(sesionPrevia));

        LoginResponse resp = authService.login(req);

        assertNotNull(resp);
        assertTrue(resp.getSesionActivaDetectada());
        assertFalse(resp.getRequiere2fa());
        assertEquals("MOVIL", resp.getDispositivoSesionActiva());
        assertTrue(resp.getMensaje().contains("MOVIL"));
        verify(otpService, never()).generarYEnviarOtp(any(), any());
    }

    @Test
    @DisplayName("login: con forzarCierreSesionAnterior=true invalida sesiones previas y genera OTP")
    void login_forzarCierreSesion_invalidaYGeneraOtp() {
        LoginRequest req = new LoginRequest("titular@banco.com", "Password123", "WEB", true);

        when(usuarioRepository.findByCorreo("titular@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123", "$2a$10$hash")).thenReturn(true);
        when(sesionService.obtenerSesionActivaPrevia(1L)).thenReturn(Optional.of(SesionActiva.builder().dispositivo("MOVIL").build()));
        when(otpService.generarYEnviarOtp(usuario, "LOGIN_2FA")).thenReturn(300);

        LoginResponse resp = authService.login(req);

        assertNotNull(resp);
        assertTrue(resp.getRequiere2fa());
        verify(sesionService).invalidarSesionesPrevias(1L);
        verify(otpService).generarYEnviarOtp(usuario, "LOGIN_2FA");
    }

    @Test
    @DisplayName("login: usuario inexistente lanza UsuarioNoEncontradoException")
    void login_usuarioNoEncontrado() {
        LoginRequest req = new LoginRequest("noexiste@banco.com", "pass", "WEB", false);
        when(usuarioRepository.findByCorreo("noexiste@banco.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("login: usuario inactivo lanza UsuarioInactivoException")
    void login_usuarioInactivo() {
        usuario.setActivo(false);
        LoginRequest req = new LoginRequest("titular@banco.com", "pass", "WEB", false);
        when(usuarioRepository.findByCorreo("titular@banco.com")).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioInactivoException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("login: contraseña incorrecta lanza CredencialesInvalidasException")
    void login_passwordIncorrecto() {
        LoginRequest req = new LoginRequest("titular@banco.com", "badPass", "WEB", false);
        when(usuarioRepository.findByCorreo("titular@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("badPass", "$2a$10$hash")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("verificarOtp: código válido emite JWT y registra nueva sesión única (Paso 2)")
    void verificarOtp_exitoso() {
        VerificarOtpRequest req = new VerificarOtpRequest("titular@banco.com", "123456", "MOVIL", "10.0.0.1");

        when(usuarioRepository.findByCorreo("titular@banco.com")).thenReturn(Optional.of(usuario));
        when(otpService.validarOtp(usuario, "123456", "LOGIN_2FA")).thenReturn(true);
        when(jwtService.generarToken(any(), anyString())).thenReturn("jwt.token.firmado");

        LoginResponse resp = authService.verificarOtp(req);

        assertNotNull(resp);
        assertEquals("jwt.token.firmado", resp.getToken());
        assertEquals("Bearer", resp.getTipo());
        assertFalse(resp.getRequiere2fa());
        verify(sesionService).registrarNuevaSesion(eq(usuario), anyString(), eq("MOVIL"), eq("10.0.0.1"));
    }

    @Test
    @DisplayName("logout: extrae JTI y cierra sesión en base de datos")
    void logout_exitoso() {
        when(jwtService.extraerJti("raw-token")).thenReturn("jti-123");
        when(sesionService.cerrarSesion("jti-123")).thenReturn(true);

        boolean resultado = authService.logout("Bearer raw-token");

        assertTrue(resultado);
        verify(sesionService).cerrarSesion("jti-123");
    }

    @Test
    @DisplayName("forzarCierreSesion: invalida sesiones por correo de usuario")
    void forzarCierreSesion_exitoso() {
        when(usuarioRepository.findByCorreo("titular@banco.com")).thenReturn(Optional.of(usuario));
        when(sesionService.invalidarSesionesPrevias(1L)).thenReturn(1);

        assertTrue(authService.forzarCierreSesion("titular@banco.com"));
        assertFalse(authService.forzarCierreSesion("otro@banco.com"));
    }
}

