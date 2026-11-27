package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.config.security.JwtService;
import com.proyecto.servicios.dto.request.auth.LoginRequest;
import com.proyecto.servicios.dto.request.auth.VerificarOtpRequest;
import com.proyecto.servicios.dto.response.auth.LoginResponse;
import com.proyecto.servicios.entity.banco.SesionActiva;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.CredencialesInvalidasException;
import com.proyecto.servicios.exception.banco.PinInvalidoException;
import com.proyecto.servicios.exception.banco.UsuarioInactivoException;
import com.proyecto.servicios.exception.banco.UsuarioNoEncontradoException;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.service.banco.SesionService;
import com.proyecto.servicios.util.TotpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: AuthServiceImpl (Flujo 2FA Google Authenticator y Sesión Única)")
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
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
    @DisplayName("login: usuario sin TOTP previo genera secreto, URI y QR de Google Authenticator sin enviar correo")
    void login_exitoso_solicita2fa() {
        LoginRequest req = new LoginRequest("titular@banco.com", "Password123", "MOVIL", false);

        when(usuarioRepository.findByCorreo("titular@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123", "$2a$10$hash")).thenReturn(true);
        when(sesionService.obtenerSesionActivaPrevia(1L)).thenReturn(Optional.empty());

        LoginResponse resp = authService.login(req);

        assertNotNull(resp);
        assertTrue(resp.getRequiere2fa());
        assertFalse(resp.getSesionActivaDetectada());
        assertEquals("titular@banco.com", resp.getCorreo());
        assertEquals("GOOGLE_AUTHENTICATOR", resp.getMetodo2fa());
        assertTrue(resp.getRequiereConfiguracionTotp());
        assertNotNull(resp.getTotpSecreto());
        assertFalse(resp.getTotpSecreto().isBlank());
        assertNotNull(resp.getTotpOtpAuthUri());
        assertTrue(resp.getTotpOtpAuthUri().startsWith("otpauth://totp/"));
        assertNotNull(resp.getTotpQrBase64());
        assertFalse(resp.getTotpQrBase64().isBlank());
        verify(usuarioRepository).save(usuario);
        assertEquals(resp.getTotpSecreto(), usuario.getTotpSecret());
        assertFalse(Boolean.TRUE.equals(usuario.getTotpHabilitado()));
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
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("login: con forzarCierreSesionAnterior=true invalida sesiones previas y entrega configuración TOTP")
    void login_forzarCierreSesion_invalidaYGeneraOtp() {
        LoginRequest req = new LoginRequest("titular@banco.com", "Password123", "WEB", true);

        when(usuarioRepository.findByCorreo("titular@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123", "$2a$10$hash")).thenReturn(true);
        when(sesionService.obtenerSesionActivaPrevia(1L)).thenReturn(Optional.of(SesionActiva.builder().dispositivo("MOVIL").build()));

        LoginResponse resp = authService.login(req);

        assertNotNull(resp);
        assertTrue(resp.getRequiere2fa());
        assertEquals("GOOGLE_AUTHENTICATOR", resp.getMetodo2fa());
        assertTrue(resp.getRequiereConfiguracionTotp());
        verify(sesionService).invalidarSesionesPrevias(1L);
        verify(usuarioRepository).save(usuario);
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
    @DisplayName("verificarOtp: código TOTP válido en primera vinculación activa totpHabilitado, emite JWT y registra sesión")
    void verificarOtp_exitoso() {
        String secreto = TotpUtil.generarSecretoBase32();
        usuario.setTotpSecret(secreto);
        usuario.setTotpHabilitado(false);
        String codigoActual = TotpUtil.generarCodigo(secreto, Math.floorDiv(Instant.now().getEpochSecond(), TotpUtil.PERIODO_SEGUNDOS));
        VerificarOtpRequest req = new VerificarOtpRequest("titular@banco.com", codigoActual, "MOVIL", "10.0.0.1");

        when(usuarioRepository.findByCorreo("titular@banco.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generarToken(any(), anyString())).thenReturn("jwt.token.firmado");

        LoginResponse resp = authService.verificarOtp(req);

        assertNotNull(resp);
        assertEquals("jwt.token.firmado", resp.getToken());
        assertEquals("Bearer", resp.getTipo());
        assertFalse(resp.getRequiere2fa());
        assertTrue(Boolean.TRUE.equals(usuario.getTotpHabilitado()));
        verify(usuarioRepository).save(usuario);
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

    @Test
    @DisplayName("login y verificarOtp: con Google Authenticator activo solicita código sin exponer secreto ni QR")
    void loginYVerificarTotp_exitoso() {
        String secreto = TotpUtil.generarSecretoBase32();
        usuario.setTotpSecret(secreto);
        usuario.setTotpHabilitado(true);

        LoginRequest loginReq = new LoginRequest("titular@banco.com", "Password123", "WEB", false);
        when(usuarioRepository.findByCorreo("titular@banco.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123", "$2a$10$hash")).thenReturn(true);
        when(sesionService.obtenerSesionActivaPrevia(1L)).thenReturn(Optional.empty());

        LoginResponse loginResp = authService.login(loginReq);
        assertTrue(loginResp.getRequiere2fa());
        assertEquals("GOOGLE_AUTHENTICATOR", loginResp.getMetodo2fa());
        assertFalse(loginResp.getRequiereConfiguracionTotp());
        assertNull(loginResp.getTotpSecreto());
        assertNull(loginResp.getTotpQrBase64());
        verify(usuarioRepository, never()).save(any());

        String codigoActual = TotpUtil.generarCodigo(secreto, Math.floorDiv(Instant.now().getEpochSecond(), TotpUtil.PERIODO_SEGUNDOS));
        when(jwtService.generarToken(any(), anyString())).thenReturn("jwt.totp.ok");

        LoginResponse otpResp = authService.verificarOtp(new VerificarOtpRequest("titular@banco.com", codigoActual, "WEB", "127.0.0.1"));
        assertEquals("jwt.totp.ok", otpResp.getToken());

        assertThrows(PinInvalidoException.class,
                () -> authService.verificarOtp(new VerificarOtpRequest("titular@banco.com", "000000".equals(codigoActual) ? "999999" : "000000", "WEB", "127.0.0.1")));
    }
}

