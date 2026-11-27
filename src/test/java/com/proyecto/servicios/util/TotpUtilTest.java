package com.proyecto.servicios.util;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Utilería TOTP: TotpUtil")
class TotpUtilTest {

    @Test
    @DisplayName("generarSecretoBase32 genera secreto Base32 válido de 32 caracteres")
    void generarSecretoBase32_valido() {
        String secreto = TotpUtil.generarSecretoBase32();
        assertNotNull(secreto);
        assertEquals(32, secreto.length());
        assertTrue(secreto.matches("^[A-Z2-7]+$"));
        assertEquals(20, TotpUtil.decodificarBase32(secreto).length);
    }

    @Test
    @DisplayName("generarCodigo y verificarCodigo validan código RFC 6238 y rechazan códigos erróneos")
    void generarYVerificarCodigo() {
        // Secreto RFC 6238 de ejemplo ("12345678901234567890") en Base32
        String secreto = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        long epochSegundos = 1_700_000_010L;
        long paso = Math.floorDiv(epochSegundos, TotpUtil.PERIODO_SEGUNDOS);

        String codigo = TotpUtil.generarCodigo(secreto, paso);
        assertEquals(6, codigo.length());
        assertTrue(codigo.matches("\\d{6}"));
        assertTrue(TotpUtil.verificarCodigo(secreto, codigo, epochSegundos));
        assertTrue(TotpUtil.verificarCodigo(secreto, codigo, epochSegundos + 30));
        assertFalse(TotpUtil.verificarCodigo(secreto, "000000".equals(codigo) ? "999999" : "000000", epochSegundos));
        assertFalse(TotpUtil.verificarCodigo(secreto, "12ab56", epochSegundos));
    }

    @Test
    @DisplayName("construirOtpAuthUri construye URI otpauth estándar para Google Authenticator")
    void construirOtpAuthUri_formatoCorrecto() {
        String uri = TotpUtil.construirOtpAuthUri("usuario@outlook.com", "ABCDEF234567");
        assertTrue(uri.startsWith("otpauth://totp/AppHasani"));
        assertTrue(uri.contains("secret=ABCDEF234567"));
        assertTrue(uri.contains("issuer=AppHasani"));
        assertTrue(uri.contains("digits=6"));
        assertTrue(uri.contains("period=30"));
    }

    @Test
    @DisplayName("generarQrPngBase64 genera PNG decodificable con la URI otpauth exacta para correos no Gmail")
    void generarQrPngBase64_decodificaUriExacta() throws Exception {
        String uri = TotpUtil.construirOtpAuthUri("usuario@outlook.com", "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        String base64 = TotpUtil.generarQrPngBase64(uri);

        assertNotNull(base64);
        assertFalse(base64.isBlank());
        byte[] pngBytes = Base64.getDecoder().decode(base64);
        assertTrue(pngBytes.length > 8);
        assertEquals((byte) 0x89, pngBytes[0]);
        assertEquals((byte) 'P', pngBytes[1]);
        assertEquals((byte) 'N', pngBytes[2]);
        assertEquals((byte) 'G', pngBytes[3]);

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(pngBytes));
        assertNotNull(img);
        int[] pixels = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        RGBLuminanceSource source = new RGBLuminanceSource(img.getWidth(), img.getHeight(), pixels);
        Result decoded = new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(source)));
        assertEquals(uri, decoded.getText());
    }
}
