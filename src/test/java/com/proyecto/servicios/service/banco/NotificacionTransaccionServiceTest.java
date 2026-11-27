package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.service.banco.NotificacionTransaccionService.NotificacionTransaccionData;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionTransaccionService")
class NotificacionTransaccionServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    private NotificacionTransaccionService crearServicio() {
        NotificacionTransaccionService service = new NotificacionTransaccionService(mailSenderProvider);
        ReflectionTestUtils.setField(service, "mailFrom", "no-reply@apphasani.com");
        return service;
    }

    @Test
    @DisplayName("Envía correo con comprobante cuando la transacción es exitosa")
    void enviarCorreoSync_transaccionExitosa_enviaMimeMessage() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificacionTransaccionService service = crearServicio();
        service.enviarCorreoSync(new NotificacionTransaccionData(
                "cliente@correo.com",
                true,
                "Telcel 100",
                "1234567890",
                "0120000008278",
                "5512345678",
                new BigDecimal("100.00"),
                new BigDecimal("900.00"),
                "AUTH-777",
                "GP-888",
                "Operación realizada con éxito",
                OffsetDateTime.now()
        ));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage enviado = captor.getValue();
        assertTrue(enviado.getSubject().contains("Comprobante de pago exitoso"));
        assertTrue(enviado.getSubject().contains("1234567890"));
    }

    @Test
    @DisplayName("Envía correo con motivo y UPC cuando la transacción es rechazada")
    void enviarCorreoSync_transaccionRechazada_enviaMimeMessage() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificacionTransaccionService service = crearServicio();
        service.enviarCorreoSync(new NotificacionTransaccionData(
                "cliente@correo.com",
                false,
                "ABIB 100",
                "8694244486",
                "0129404198278",
                "4681372815",
                new BigDecimal("100.00"),
                new BigDecimal("1000.00"),
                "-1",
                "-1",
                "Transacción rechazada por el proveedor",
                OffsetDateTime.now()
        ));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage enviado = captor.getValue();
        assertTrue(enviado.getSubject().contains("Aviso de pago rechazado"));
        assertTrue(enviado.getSubject().contains("8694244486"));
    }

    @Test
    @DisplayName("Un fallo SMTP no propaga excepción ni interrumpe el flujo de pago")
    void enviarCorreoSync_errorSmtp_noLanzaExcepcion() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP caído"));

        NotificacionTransaccionService service = crearServicio();
        assertDoesNotThrow(() -> service.enviarCorreoSync(new NotificacionTransaccionData(
                "cliente@correo.com",
                false,
                "CFE",
                "9988776655",
                "0120000001234",
                "1234567890",
                new BigDecimal("250.00"),
                new BigDecimal("1000.00"),
                null,
                null,
                "Fondos insuficientes",
                OffsetDateTime.now()
        )));
    }
}
