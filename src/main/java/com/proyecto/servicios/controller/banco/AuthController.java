package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.service.banco.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión y generación de tokens JWT")
public class AuthController {

    private final AuthService authService;

    @Data
    public static class LoginRequest {
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        private String correo;

        @NotBlank(message = "La contraseña es obligatoria")
        private String password;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener Bearer Token JWT")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.getCorreo(), request.getPassword()));
    }
}
