package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.banco.PinInvalidoException;
import com.proyecto.servicios.exception.banco.UsuarioNoEncontradoException;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.util.TotpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/seguridad/totp")
@RequiredArgsConstructor
@Tag(name = "Seguridad 2FA (TOTP)", description = "Vinculación y gestión de Google Authenticator")
public class SeguridadTotpController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping("/estado")
    @Operation(summary = "Consultar si el usuario autenticado tiene Google Authenticator activo")
    public ResponseEntity<GenericResponse<Map<String, Object>>> estado(Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        boolean habilitado = Boolean.TRUE.equals(usuario.getTotpHabilitado())
                && usuario.getTotpSecret() != null
                && !usuario.getTotpSecret().isBlank();
        return ResponseEntity.ok(GenericResponse.exito(
                "Estado de Google Authenticator consultado",
                Map.of("habilitado", habilitado)
        ));
    }

    @PostMapping("/configurar")
    @Transactional
    @Operation(summary = "Generar clave secreta y URI otpauth:// para vincular Google Authenticator")
    public ResponseEntity<GenericResponse<Map<String, Object>>> configurar(Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        String secreto = TotpUtil.generarSecretoBase32();
        usuario.setTotpSecret(secreto);
        usuario.setTotpHabilitado(false);
        usuarioRepository.save(usuario);

        String otpAuthUri = TotpUtil.construirOtpAuthUri(usuario.getCorreo(), secreto);
        String qrBase64 = TotpUtil.generarQrPngBase64(otpAuthUri);
        return ResponseEntity.ok(GenericResponse.exito(
                "Clave generada. Escanea el código QR o usa la clave manual e ingresa un código para confirmar.",
                Map.of(
                        "secreto", secreto,
                        "otpAuthUri", otpAuthUri,
                        "qrBase64", qrBase64,
                        "emisor", "AppHasani",
                        "correo", usuario.getCorreo()
                )
        ));
    }

    @PostMapping("/activar")
    @Transactional
    @Operation(summary = "Verificar código de 6 dígitos y activar Google Authenticator")
    public ResponseEntity<GenericResponse<Map<String, Object>>> activar(
            Authentication authentication,
            @RequestBody Map<String, String> payload) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        if (usuario.getTotpSecret() == null || usuario.getTotpSecret().isBlank()) {
            throw new ReglaNegocioException("Primero genera una clave de configuración para Google Authenticator.");
        }

        String codigo = payload != null ? payload.get("codigo") : null;
        if (!TotpUtil.verificarCodigo(usuario.getTotpSecret(), codigo)) {
            throw new PinInvalidoException("El código de 6 dígitos es incorrecto. Verifica la hora de tu dispositivo e intenta de nuevo.");
        }

        usuario.setTotpHabilitado(true);
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(GenericResponse.exito(
                "Google Authenticator activado correctamente",
                Map.of("habilitado", true)
        ));
    }

    @PostMapping("/desactivar")
    @Operation(summary = "Deshabilitado: Google Authenticator es el único método de verificación 2FA")
    public ResponseEntity<GenericResponse<Map<String, Object>>> desactivar(
            Authentication authentication,
            @RequestBody(required = false) Map<String, String> payload) {
        obtenerUsuarioAutenticado(authentication);
        throw new ReglaNegocioException(
                "La verificación por correo está deshabilitada. Usa la opción de reconfigurar para vincular Google Authenticator en otro dispositivo.");
    }

    private Usuario obtenerUsuarioAutenticado(Authentication authentication) {
        String correo = authentication != null ? authentication.getName() : null;
        if (correo == null || correo.isBlank()) {
            throw new UsuarioNoEncontradoException("No se pudo identificar al usuario autenticado.");
        }
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado: " + correo));
    }
}
