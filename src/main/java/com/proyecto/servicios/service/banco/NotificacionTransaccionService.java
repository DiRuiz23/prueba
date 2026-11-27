package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.util.EmailPlantillaHelper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionTransaccionService {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public record NotificacionTransaccionData(
            String correoDestino,
            boolean exitosa,
            String nombreServicio,
            String upc,
            String numeroCuenta,
            String telefonoReferencia,
            BigDecimal monto,
            BigDecimal saldoRestante,
            String numeroAutorizacion,
            String idTxGestoPago,
            String motivo,
            OffsetDateTime fechaTransaccion
    ) {
    }

    public void notificarResultadoTransaccion(NotificacionTransaccionData data) {
        if (data == null || data.correoDestino() == null || data.correoDestino().isBlank()) {
            return;
        }
        CompletableFuture.runAsync(() -> enviarCorreoSync(data));
    }

    void enviarCorreoSync(NotificacionTransaccionData data) {
        if (data == null || data.correoDestino() == null || data.correoDestino().isBlank()) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider != null ? mailSenderProvider.getIfAvailable() : null;
        if (mailSender == null) {
            log.info("JavaMailSender no disponible. Se omite notificación de transacción para {}.", data.correoDestino());
            return;
        }

        String destinatario = data.correoDestino().trim().toLowerCase(Locale.ROOT);
        String upc = (data.upc() == null || data.upc().isBlank()) ? "No generado" : data.upc().trim();
        String servicio = (data.nombreServicio() == null || data.nombreServicio().isBlank())
                ? "Pago de servicio"
                : data.nombreServicio().trim();
        String montoFmt = formatearMoneda(data.monto());
        String saldoFmt = data.saldoRestante() != null ? formatearMoneda(data.saldoRestante()) : null;
        String cuentaMask = enmascararCuenta(data.numeroCuenta());
        String referencia = data.telefonoReferencia() != null ? data.telefonoReferencia().trim() : "—";
        String autorizacion = data.exitosa() ? limpiarCentinela(data.numeroAutorizacion()) : null;
        String folio = limpiarCentinela(data.idTxGestoPago());
        OffsetDateTime fecha = data.fechaTransaccion() != null
                ? data.fechaTransaccion()
                : OffsetDateTime.now(ZoneId.systemDefault());
        String fechaFmt = FECHA_FMT.format(fecha.atZoneSameInstant(ZoneId.systemDefault()));

        String asunto = data.exitosa()
                ? construirAsunto("Comprobante de pago exitoso — AppHasani", upc)
                : construirAsunto("Aviso de pago rechazado — AppHasani", upc);

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom, "AppHasani");
                helper.setReplyTo(mailFrom);
            }
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(EmailPlantillaHelper.construirHtmlResultadoTransaccion(
                    data.exitosa(),
                    servicio,
                    montoFmt,
                    saldoFmt,
                    upc,
                    cuentaMask,
                    referencia,
                    autorizacion,
                    folio,
                    data.motivo(),
                    fechaFmt
            ), true);
            mailSender.send(mime);
            log.info("Correo de resultado ({}) enviado a {} (UPC: {}).",
                    data.exitosa() ? "EXITOSA" : "RECHAZADA", destinatario, upc);
        } catch (Exception e) {
            log.warn("No se pudo enviar notificación de transacción a {} (UPC: {}): {}",
                    destinatario, upc, e.getMessage());
        }
    }

    private String construirAsunto(String base, String upc) {
        if ("No generado".equals(upc)) {
            return base;
        }
        return base + " (UPC " + upc + ")";
    }

    private String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return "$" + df.format(monto) + " MXN";
    }

    private String enmascararCuenta(String cuenta) {
        if (cuenta == null || cuenta.isBlank()) {
            return "—";
        }
        String limpia = cuenta.trim();
        if (limpia.length() >= 4) {
            return "•••• " + limpia.substring(limpia.length() - 4);
        }
        return limpia;
    }

    private String limpiarCentinela(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return (limpio.isEmpty() || "-1".equals(limpio) || "0".equals(limpio)) ? null : limpio;
    }
}
