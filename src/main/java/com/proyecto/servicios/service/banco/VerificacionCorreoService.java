package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.banco.CorreoElectronicoDuplicadoException;
import com.proyecto.servicios.exception.banco.PinBloqueadoException;
import com.proyecto.servicios.exception.banco.PinInvalidoException;
import com.proyecto.servicios.repository.banco.ClienteRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.util.EmailPlantillaHelper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificacionCorreoService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern PATRON_EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    public static final int EXPIRACION_SEGUNDOS = 300;
    public static final int MAX_INTENTOS = 3;

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Map<String, CodigoVerificacion> codigosActivos = new ConcurrentHashMap<>();

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public int enviarCodigoVerificacion(String correo) {
        String norm = normalizarYValidarCorreo(correo);

        if (clienteRepository.existsByCorreo(norm) || usuarioRepository.existsByCorreo(norm)) {
            throw new CorreoElectronicoDuplicadoException("El correo '" + norm + "' ya se encuentra registrado.");
        }

        String codigoPlano = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
        OffsetDateTime expiracion = OffsetDateTime.now(ZoneId.systemDefault()).plusSeconds(EXPIRACION_SEGUNDOS);

        codigosActivos.put(norm, new CodigoVerificacion(calcularSha256(codigoPlano), expiracion, 0, false));
        enviarCorreo(norm, codigoPlano);

        log.info("Código de verificación pre-registro generado para {} (código: [{}])", norm, codigoPlano);
        return EXPIRACION_SEGUNDOS;
    }

    public boolean confirmarCodigoVerificacion(String correo, String codigo) {
        String norm = normalizarYValidarCorreo(correo);
        CodigoVerificacion registro = codigosActivos.get(norm);
        if (registro == null) {
            throw new ReglaNegocioException("Solicita primero un código de verificación para este correo.");
        }

        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.systemDefault());
        if (registro.expiracion.isBefore(ahora)) {
            codigosActivos.remove(norm);
            throw new ReglaNegocioException("El código de verificación expiró. Solicita uno nuevo.");
        }

        String hashEntrada = calcularSha256(codigo != null ? codigo.trim() : "");
        if (!hashEntrada.equalsIgnoreCase(registro.codigoHash)) {
            int intentos = registro.intentos + 1;
            if (intentos >= MAX_INTENTOS) {
                codigosActivos.remove(norm);
                throw new PinBloqueadoException("Superaste los 3 intentos permitidos. Solicita un nuevo código.");
            }
            codigosActivos.put(norm, new CodigoVerificacion(registro.codigoHash, registro.expiracion, intentos, false));
            throw new PinInvalidoException("Código incorrecto. Intentos restantes: " + (MAX_INTENTOS - intentos));
        }

        codigosActivos.put(norm, new CodigoVerificacion(registro.codigoHash, registro.expiracion, registro.intentos, true));
        return true;
    }

    private void enviarCorreo(String destinatario, String codigo) {
        JavaMailSender mailSender = mailSenderProvider != null ? mailSenderProvider.getIfAvailable() : null;
        if (mailSender == null) {
            log.info("JavaMailSender no disponible en el entorno. Se omite envío SMTP real para verificación de {}", destinatario);
            return;
        }
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom, "AppHasani");
                helper.setReplyTo(mailFrom);
            }
            helper.setTo(destinatario);
            helper.setSubject("Confirma tu correo — AppHasani");
            helper.setText(EmailPlantillaHelper.construirHtmlCodigo(
                    "Confirma tu correo electrónico",
                    "Ingresa este código en tu formulario de registro para verificar que tu correo es válido.",
                    codigo,
                    EXPIRACION_SEGUNDOS / 60
            ), true);
            mailSender.send(mime);
        } catch (Exception e) {
            codigosActivos.remove(destinatario);
            log.warn("No se pudo enviar código de verificación al correo {}: {}", destinatario, e.getMessage());
            throw new ReglaNegocioException(
                    "No fue posible enviar el código al correo indicado. Verifica que el correo exista y esté bien escrito.");
        }
    }

    private String normalizarYValidarCorreo(String correo) {
        String norm = correo == null ? "" : correo.trim().toLowerCase();
        if (norm.isEmpty() || !PATRON_EMAIL.matcher(norm).matches()) {
            throw new ReglaNegocioException("El formato del correo electrónico no es válido.");
        }
        return norm;
    }

    private String calcularSha256(String entrada) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(entrada.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }

    private record CodigoVerificacion(String codigoHash, OffsetDateTime expiracion, int intentos, boolean verificado) {
    }
}
