package com.proyecto.servicios.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

/**
 * Implementación RFC 6238 / RFC 4226 para 2FA compatible con Google Authenticator.
 */
public final class TotpUtil {

    public static final int DIGITOS = 6;
    public static final int PERIODO_SEGUNDOS = 30;
    public static final int VENTANA_TOLERANCIA = 1;

    private static final String ALFABETO_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String EMISOR = "AppHasani";

    private TotpUtil() {
    }

    public static String generarSecretoBase32() {
        byte[] bytes = new byte[20];
        SECURE_RANDOM.nextBytes(bytes);
        return codificarBase32(bytes);
    }

    public static boolean verificarCodigo(String secretoBase32, String codigoIngresado) {
        return verificarCodigo(secretoBase32, codigoIngresado, Instant.now().getEpochSecond());
    }

    public static boolean verificarCodigo(String secretoBase32, String codigoIngresado, long epochSegundos) {
        if (secretoBase32 == null || secretoBase32.isBlank() || codigoIngresado == null) {
            return false;
        }
        String limpio = codigoIngresado.trim();
        if (!limpio.matches("\\d{" + DIGITOS + "}")) {
            return false;
        }

        long pasoActual = Math.floorDiv(epochSegundos, PERIODO_SEGUNDOS);
        byte[] entradaBytes = limpio.getBytes(StandardCharsets.US_ASCII);

        for (long delta = -VENTANA_TOLERANCIA; delta <= VENTANA_TOLERANCIA; delta++) {
            String esperado = generarCodigo(secretoBase32, pasoActual + delta);
            if (MessageDigest.isEqual(entradaBytes, esperado.getBytes(StandardCharsets.US_ASCII))) {
                return true;
            }
        }
        return false;
    }

    public static String generarCodigo(String secretoBase32, long pasoTiempo) {
        byte[] clave = decodificarBase32(secretoBase32);
        byte[] datos = ByteBuffer.allocate(8).putLong(pasoTiempo).array();

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(clave, "HmacSHA1"));
            byte[] hmac = mac.doFinal(datos);

            int offset = hmac[hmac.length - 1] & 0x0F;
            int binario = ((hmac[offset] & 0x7F) << 24)
                    | ((hmac[offset + 1] & 0xFF) << 16)
                    | ((hmac[offset + 2] & 0xFF) << 8)
                    | (hmac[offset + 3] & 0xFF);

            int otp = binario % (int) Math.pow(10, DIGITOS);
            return String.format("%0" + DIGITOS + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular código TOTP", e);
        }
    }

    public static String construirOtpAuthUri(String correo, String secretoBase32) {
        String cuenta = correo == null ? "" : correo.trim().toLowerCase();
        String label = urlEncode(EMISOR + ":" + cuenta).replace("+", "%20");
        String issuer = urlEncode(EMISOR).replace("+", "%20");
        return "otpauth://totp/" + label
                + "?secret=" + secretoBase32
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=" + DIGITOS
                + "&period=" + PERIODO_SEGUNDOS;
    }

    public static String generarQrPngBase64(String otpAuthUri) {
        return generarQrPngBase64(otpAuthUri, 240);
    }

    public static String generarQrPngBase64(String otpAuthUri, int tamanio) {
        if (otpAuthUri == null || otpAuthUri.isBlank()) {
            throw new IllegalArgumentException("URI otpauth vacía");
        }
        int dimension = Math.max(120, tamanio);
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 2
            );
            BitMatrix matrix = new QRCodeWriter().encode(
                    otpAuthUri, BarcodeFormat.QR_CODE, dimension, dimension, hints);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xFF111827 : 0xFFFFFFFF);
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No se pudo generar el código QR para Google Authenticator", e);
        }
    }

    private static String urlEncode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }

    public static String codificarBase32(byte[] datos) {
        StringBuilder sb = new StringBuilder((datos.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsEnBuffer = 0;

        for (byte b : datos) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsEnBuffer += 8;
            while (bitsEnBuffer >= 5) {
                int indice = (buffer >> (bitsEnBuffer - 5)) & 0x1F;
                sb.append(ALFABETO_BASE32.charAt(indice));
                bitsEnBuffer -= 5;
            }
        }
        if (bitsEnBuffer > 0) {
            int indice = (buffer << (5 - bitsEnBuffer)) & 0x1F;
            sb.append(ALFABETO_BASE32.charAt(indice));
        }
        return sb.toString();
    }

    public static byte[] decodificarBase32(String base32) {
        if (base32 == null) {
            throw new IllegalArgumentException("Secreto Base32 nulo");
        }
        String limpio = base32.trim().replace("=", "").replaceAll("\\s+", "").toUpperCase();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("Secreto Base32 vacío");
        }

        byte[] salida = new byte[(limpio.length() * 5) / 8];
        int buffer = 0;
        int bitsEnBuffer = 0;
        int pos = 0;

        for (int i = 0; i < limpio.length(); i++) {
            int valor = ALFABETO_BASE32.indexOf(limpio.charAt(i));
            if (valor < 0) {
                throw new IllegalArgumentException("Carácter inválido en secreto Base32");
            }
            buffer = (buffer << 5) | valor;
            bitsEnBuffer += 5;
            if (bitsEnBuffer >= 8) {
                salida[pos++] = (byte) ((buffer >> (bitsEnBuffer - 8)) & 0xFF);
                bitsEnBuffer -= 8;
            }
        }
        return pos == salida.length ? salida : Arrays.copyOf(salida, pos);
    }
}
