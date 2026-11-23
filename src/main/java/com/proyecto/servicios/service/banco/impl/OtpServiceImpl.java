package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.banco.OtpToken;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.banco.PinBloqueadoException;
import com.proyecto.servicios.exception.banco.PinInvalidoException;
import com.proyecto.servicios.repository.banco.OtpTokenRepository;
import com.proyecto.servicios.service.banco.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final int EXPIRACION_SEGUNDOS = 300; // 5 minutos
    public static final int MAX_INTENTOS_OTP = 3;
    public static final String PROPOSITO_LOGIN_2FA = "LOGIN_2FA";

    private final OtpTokenRepository otpTokenRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Override
    @Transactional
    public int generarYEnviarOtp(Usuario usuario, String proposito) {
        String prop = (proposito != null && !proposito.isBlank()) ? proposito : PROPOSITO_LOGIN_2FA;

        // 1. Invalidar OTPs previos no usados para este usuario y propósito
        otpTokenRepository.invalidarOtpsPrevios(usuario.getId(), prop);

        // 2. Generar código criptográficamente seguro de 6 dígitos
        int codigoInt = 100000 + SECURE_RANDOM.nextInt(900000);
        String codigoStr = String.valueOf(codigoInt);

        // 3. Hashear el código con SHA-256 antes de persistir
        String hash = calcularSha256(codigoStr);

        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.systemDefault());
        OffsetDateTime expiracion = ahora.plusSeconds(EXPIRACION_SEGUNDOS);

        OtpToken otpToken = OtpToken.builder()
                .usuario(usuario)
                .codigoHash(hash)
                .proposito(prop)
                .intentos(0)
                .maxIntentos(MAX_INTENTOS_OTP)
                .usado(false)
                .bloqueado(false)
                .fechaCreacion(ahora)
                .fechaExpiracion(expiracion)
                .build();

        otpTokenRepository.save(otpToken);

        // 4. Envío de correo o log seguro
        enviarCorreoSeguro(usuario.getCorreo(), codigoStr, prop);

        log.info("Código OTP 2FA generado exitosamente para {} (propósito: {}, expira en {} segundos). Código para evaluación: [{}]",
                usuario.getCorreo(), prop, EXPIRACION_SEGUNDOS, codigoStr);

        return EXPIRACION_SEGUNDOS;
    }

    @Override
    @Transactional
    public boolean validarOtp(Usuario usuario, String codigoPlano, String proposito) {
        String prop = (proposito != null && !proposito.isBlank()) ? proposito : PROPOSITO_LOGIN_2FA;

        Optional<OtpToken> optOtp = otpTokenRepository
                .findFirstByUsuarioIdAndPropositoAndUsadoFalseAndBloqueadoFalseOrderByFechaCreacionDesc(
                        usuario.getId(), prop);

        if (optOtp.isEmpty()) {
            throw new ReglaNegocioException("No existe un código de verificación activo. Solicite uno nuevo.");
        }

        OtpToken otp = optOtp.get();
        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.systemDefault());

        // Verificar expiración
        if (otp.getFechaExpiracion().isBefore(ahora)) {
            otp.setUsado(true);
            otpTokenRepository.save(otp);
            throw new ReglaNegocioException("El código de verificación ha expirado. Solicite uno nuevo.");
        }

        // Verificar hash
        String hashEntrada = calcularSha256(codigoPlano != null ? codigoPlano.trim() : "");
        if (!hashEntrada.equalsIgnoreCase(otp.getCodigoHash())) {
            int intentosActuales = otp.getIntentos() + 1;
            otp.setIntentos(intentosActuales);

            if (intentosActuales >= otp.getMaxIntentos()) {
                otp.setBloqueado(true);
                otpTokenRepository.save(otp);
                log.warn("Código OTP bloqueado por superar límite de intentos para usuario {}", usuario.getCorreo());
                throw new PinBloqueadoException("Ha superado el número máximo de intentos permitidos (3). Solicite un nuevo código.");
            }

            otpTokenRepository.save(otp);
            int restantes = otp.getMaxIntentos() - intentosActuales;
            throw new PinInvalidoException("Código de verificación incorrecto. Intentos restantes: " + restantes);
        }

        // Éxito: marcar como usado de forma irreversible
        otp.setUsado(true);
        otpTokenRepository.save(otp);
        log.info("Código OTP validado correctamente para usuario {}", usuario.getCorreo());
        return true;
    }

    private void enviarCorreoSeguro(String destinatario, String codigo, String proposito) {
        JavaMailSender mailSender = mailSenderProvider != null ? mailSenderProvider.getIfAvailable() : null;
        if (mailSender == null) {
            log.info("JavaMailSender no disponible en el entorno. Se omite envío SMTP real para: {}", destinatario);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject("Código de Seguridad 2FA - Comercio Digital");
            mensaje.setText("Hola,\n\nTu código de verificación de seguridad para " + proposito + " es:\n\n"
                    + codigo + "\n\nEste código es confidencial y vence en 5 minutos.\nSi no solicitaste este código, ignora este mensaje.");
            mailSender.send(mensaje);
            log.info("Correo de seguridad 2FA enviado exitosamente a {}", destinatario);
        } catch (Exception e) {
            log.error("No fue posible enviar el correo de verificación a {}: {}", destinatario, e.getMessage());
        }
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
}

