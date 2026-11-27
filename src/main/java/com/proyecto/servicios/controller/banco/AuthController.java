package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.dto.request.auth.LoginRequest;
import com.proyecto.servicios.dto.request.auth.VerificarOtpRequest;
import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.dto.response.auth.LoginResponse;
import com.proyecto.servicios.service.banco.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión, 2FA, gestión de sesión única y logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Paso 1: Validar credenciales y solicitar código 2FA / verificar sesión única")
    public ResponseEntity<GenericResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse respuesta = authService.login(request);
        return ResponseEntity.ok(GenericResponse.exito(respuesta.getMensaje(), respuesta));
    }

    @PostMapping("/verificar-2fa")
    @Operation(summary = "Paso 2: Validar código 2FA (OTP) y obtener Bearer Token JWT con sesión única")
    public ResponseEntity<GenericResponse<LoginResponse>> verificar2fa(
            @Valid @RequestBody VerificarOtpRequest request,
            HttpServletRequest httpRequest) {
        if (request.getIpOrigen() == null || request.getIpOrigen().isBlank()) {
            request.setIpOrigen(httpRequest.getRemoteAddr());
        }
        LoginResponse respuesta = authService.verificarOtp(request);
        return ResponseEntity.ok(GenericResponse.exito(respuesta.getMensaje(), respuesta));
    }

    @PostMapping("/forzar-cierre-sesion")
    @Operation(summary = "Finalizar sesiones previas de un usuario para permitir nuevo acceso")
    public ResponseEntity<GenericResponse<Boolean>> forzarCierreSesion(
            @RequestBody Map<String, String> payload) {
        String correo = payload.get("correo");
        boolean cerrado = authService.forzarCierreSesion(correo);
        return ResponseEntity.ok(GenericResponse.exito("Sesiones anteriores finalizadas", cerrado));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión activa e invalidar token JWT en el servidor")
    public ResponseEntity<GenericResponse<Boolean>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        boolean cerrado = authService.logout(authHeader);
        return ResponseEntity.ok(GenericResponse.exito("Sesión finalizada con éxito", cerrado));
    }
}
