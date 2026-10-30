package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.dto.request.auth.LoginRequest;
import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.dto.response.auth.LoginResponse;
import com.proyecto.servicios.service.banco.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión y generación de tokens JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener Bearer Token JWT")
    public ResponseEntity<GenericResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse respuesta = authService.login(request);
        return ResponseEntity.ok(GenericResponse.exito("Autenticación exitosa", respuesta));
    }
}
