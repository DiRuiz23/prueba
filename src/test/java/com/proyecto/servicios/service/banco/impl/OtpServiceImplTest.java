package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.banco.OtpToken;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.banco.PinBloqueadoException;
import com.proyecto.servicios.exception.banco.PinInvalidoException;
import com.proyecto.servicios.repository.banco.OtpTokenRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: OtpServiceImpl (2FA de alta seguridad)")
class OtpServiceImplTest {

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private OtpServiceImpl otpService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(5L).correo("cliente@banco.com").activo(true).build();
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("generarYEnviarOtp: invalida previos, genera hash seguro de 6 dígitos, persiste y envía correo")
    void generarYEnviarOtp_exitoso() {
        ArgumentCaptor<OtpToken> captor = ArgumentCaptor.forClass(OtpToken.class);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        int expiracion = otpService.generarYEnviarOtp(usuario, "LOGIN_2FA");

        assertEquals(300, expiracion);
        verify(otpTokenRepository).invalidarOtpsPrevios(5L, "LOGIN_2FA");
        verify(otpTokenRepository).save(captor.capture());
        verify(mailSender).send(any(MimeMessage.class));

        OtpToken guardado = captor.getValue();
        assertNotNull(guardado);
        assertNotNull(guardado.getCodigoHash());
        assertEquals(64, guardado.getCodigoHash().length());
        assertEquals("LOGIN_2FA", guardado.getProposito());
        assertFalse(guardado.getUsado());
        assertFalse(guardado.getBloqueado());
        assertEquals(0, guardado.getIntentos());
    }

    @Test
    @DisplayName("generarYEnviarOtp: funciona incluso sin JavaMailSender disponible y maneja propósito nulo")
    void generarYEnviarOtp_sinMailSender() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        int expiracion = otpService.generarYEnviarOtp(usuario, null);

        assertEquals(300, expiracion);
        verify(otpTokenRepository).invalidarOtpsPrevios(5L, OtpServiceImpl.PROPOSITO_LOGIN_2FA);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("validarOtp: código correcto marca usado y retorna true")
    void validarOtp_exitoso() {
        String codigo = "123456";
        OtpToken token = OtpToken.builder()
                .id(1L)
                .usuario(usuario)
                .codigoHash(sha256(codigo))
                .proposito("LOGIN_2FA")
                .intentos(0)
                .maxIntentos(3)
                .usado(false)
                .bloqueado(false)
                .fechaExpiracion(OffsetDateTime.now(ZoneId.systemDefault()).plusMinutes(5))
                .build();

        when(otpTokenRepository.findFirstByUsuarioIdAndPropositoAndUsadoFalseAndBloqueadoFalseOrderByFechaCreacionDesc(5L, "LOGIN_2FA"))
                .thenReturn(Optional.of(token));

        boolean resultado = otpService.validarOtp(usuario, codigo, "LOGIN_2FA");

        assertTrue(resultado);
        assertTrue(token.getUsado());
        verify(otpTokenRepository).save(token);
    }

    @Test
    @DisplayName("validarOtp: código expirado lanza ReglaNegocioException y se marca usado")
    void validarOtp_expirado() {
        OtpToken token = OtpToken.builder()
                .id(1L)
                .usuario(usuario)
                .codigoHash(sha256("123456"))
                .proposito("LOGIN_2FA")
                .usado(false)
                .bloqueado(false)
                .fechaExpiracion(OffsetDateTime.now(ZoneId.systemDefault()).minusSeconds(10))
                .build();

        when(otpTokenRepository.findFirstByUsuarioIdAndPropositoAndUsadoFalseAndBloqueadoFalseOrderByFechaCreacionDesc(5L, "LOGIN_2FA"))
                .thenReturn(Optional.of(token));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> otpService.validarOtp(usuario, "123456", "LOGIN_2FA"));

        assertTrue(ex.getMessage().contains("expirado"));
        assertTrue(token.getUsado());
        verify(otpTokenRepository).save(token);
    }

    @Test
    @DisplayName("validarOtp: código incorrecto incrementa intentos y lanza PinInvalidoException")
    void validarOtp_codigoIncorrecto() {
        OtpToken token = OtpToken.builder()
                .id(1L)
                .usuario(usuario)
                .codigoHash(sha256("654321"))
                .proposito("LOGIN_2FA")
                .intentos(0)
                .maxIntentos(3)
                .usado(false)
                .bloqueado(false)
                .fechaExpiracion(OffsetDateTime.now(ZoneId.systemDefault()).plusMinutes(5))
                .build();

        when(otpTokenRepository.findFirstByUsuarioIdAndPropositoAndUsadoFalseAndBloqueadoFalseOrderByFechaCreacionDesc(5L, "LOGIN_2FA"))
                .thenReturn(Optional.of(token));

        PinInvalidoException ex = assertThrows(PinInvalidoException.class,
                () -> otpService.validarOtp(usuario, "111111", "LOGIN_2FA"));

        assertTrue(ex.getMessage().contains("incorrecto"));
        assertEquals(1, token.getIntentos());
        verify(otpTokenRepository).save(token);
    }

    @Test
    @DisplayName("validarOtp: tercer intento fallido bloquea código y lanza PinBloqueadoException")
    void validarOtp_tercerIntentoBloquea() {
        OtpToken token = OtpToken.builder()
                .id(1L)
                .usuario(usuario)
                .codigoHash(sha256("654321"))
                .proposito("LOGIN_2FA")
                .intentos(2)
                .maxIntentos(3)
                .usado(false)
                .bloqueado(false)
                .fechaExpiracion(OffsetDateTime.now(ZoneId.systemDefault()).plusMinutes(5))
                .build();

        when(otpTokenRepository.findFirstByUsuarioIdAndPropositoAndUsadoFalseAndBloqueadoFalseOrderByFechaCreacionDesc(5L, "LOGIN_2FA"))
                .thenReturn(Optional.of(token));

        PinBloqueadoException ex = assertThrows(PinBloqueadoException.class,
                () -> otpService.validarOtp(usuario, "111111", "LOGIN_2FA"));

        assertTrue(ex.getMessage().contains("máximo de intentos"));
        assertTrue(token.getBloqueado());
        assertEquals(3, token.getIntentos());
        verify(otpTokenRepository).save(token);
    }

    @Test
    @DisplayName("validarOtp: no existe OTP activo lanza ReglaNegocioException")
    void validarOtp_noExiste() {
        when(otpTokenRepository.findFirstByUsuarioIdAndPropositoAndUsadoFalseAndBloqueadoFalseOrderByFechaCreacionDesc(5L, "LOGIN_2FA"))
                .thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class,
                () -> otpService.validarOtp(usuario, "123456", "LOGIN_2FA"));
    }
}

