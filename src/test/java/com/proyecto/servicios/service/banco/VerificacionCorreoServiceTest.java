package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.banco.CorreoElectronicoDuplicadoException;
import com.proyecto.servicios.exception.banco.PinInvalidoException;
import com.proyecto.servicios.repository.banco.ClienteRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: VerificacionCorreoService")
class VerificacionCorreoServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private VerificacionCorreoService service;

    @Test
    @DisplayName("enviarCodigoVerificacion: rechaza correo ya registrado")
    void enviarCodigo_correoDuplicado() {
        when(clienteRepository.existsByCorreo("usado@banco.com")).thenReturn(true);

        assertThrows(CorreoElectronicoDuplicadoException.class,
                () -> service.enviarCodigoVerificacion("USADO@banco.com"));
    }

    @Test
    @DisplayName("enviarCodigoVerificacion: rechaza cuando SMTP falla al enviar")
    void enviarCodigo_fallaSmtp() {
        when(clienteRepository.existsByCorreo("noexiste@invalido.test")).thenReturn(false);
        when(usuarioRepository.existsByCorreo("noexiste@invalido.test")).thenReturn(false);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP rejected")).when(mailSender).send(any(MimeMessage.class));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.enviarCodigoVerificacion("noexiste@invalido.test"));
        assertTrue(ex.getMessage().contains("No fue posible enviar el código"));
    }

    @Test
    @DisplayName("confirmarCodigoVerificacion: valida formato y rechaza código incorrecto")
    void confirmarCodigo_sinEnvioPrevio_y_formatoInvalido() {
        assertThrows(ReglaNegocioException.class,
                () -> service.confirmarCodigoVerificacion("invalido", "123456"));
        assertThrows(ReglaNegocioException.class,
                () -> service.confirmarCodigoVerificacion("nuevo@banco.com", "123456"));

        when(clienteRepository.existsByCorreo("ok@banco.com")).thenReturn(false);
        when(usuarioRepository.existsByCorreo("ok@banco.com")).thenReturn(false);
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        assertEquals(300, service.enviarCodigoVerificacion("ok@banco.com"));

        assertThrows(PinInvalidoException.class,
                () -> service.confirmarCodigoVerificacion("ok@banco.com", "000000"));
        assertNotNull(Usuario.builder().build());
    }

    @Test
    @DisplayName("enviarCodigoVerificacion: acepta correos de distintos proveedores (Outlook, Yahoo, educativos)")
    void enviarCodigo_aceptaMultiplesProveedores() {
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertEquals(300, service.enviarCodigoVerificacion("usuario@outlook.com"));
        assertEquals(300, service.enviarCodigoVerificacion("alumno@alumnos.utez.edu.mx"));
        assertEquals(300, service.enviarCodigoVerificacion("cliente+test@yahoo.com"));
    }
}
