package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.ReglaNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utilidad de Cifrado y Descifrado AES-256-CBC con relleno PKCS5Padding
 * para la integración transaccional con la API de PuntoRed / GestoPago.
 */
@Component
@Slf4j
public class AesCifradoUtil {

    private final String secretKeyBase64;
    private final String ivBase64;

    public AesCifradoUtil(
            @Value("${gestopago.tx.secret-key}") String secretKeyBase64,
            @Value("${gestopago.tx.iv}") String ivBase64) {
        this.secretKeyBase64 = secretKeyBase64;
        this.ivBase64 = ivBase64;
    }

    /**
     * Cifra un texto plano en formato JSON utilizando las llaves configuradas.
     *
     * @param plainText Contenido en texto plano (ej. JSON del payload).
     * @return Cadena cifrada codificada en Base64.
     */
    public String cifrar(String plainText) {
        return cifrarConLlaves(plainText, this.secretKeyBase64, this.ivBase64);
    }

    /**
     * Descifra una cadena Base64 cifrada con AES-256-CBC.
     *
     * @param cipherTextBase64 Cadena cifrada en Base64.
     * @return Texto plano descifrado en UTF-8.
     */
    public String descifrar(String cipherTextBase64) {
        return descifrarConLlaves(cipherTextBase64, this.secretKeyBase64, this.ivBase64);
    }

    public static String cifrarConLlaves(String plainText, String keyBase64, String ivBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64.trim());
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64.trim());

            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            log.error("Error al cifrar el payload con AES: {}", e.getMessage(), e);
            throw new ReglaNegocioException("Error interno al firmar y cifrar la transacción: " + e.getMessage());
        }
    }

    public static String descifrarConLlaves(String cipherTextBase64, String keyBase64, String ivBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64.trim());
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64.trim());

            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherTextBase64.trim()));
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Error al descifrar con AES: {}", e.getMessage(), e);
            throw new ReglaNegocioException("Error al descifrar la información transaccional: " + e.getMessage());
        }
    }
}
