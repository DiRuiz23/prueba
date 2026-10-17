package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.dto.request.auth.LoginRequest;
import com.proyecto.servicios.dto.response.auth.LoginResponse;

/**
 * Servicio de autenticación. Centraliza la lógica de login y generación de JWT.
 */
public interface AuthService {

    /**
     * Autentica a un usuario y genera un token JWT.
     *
     * @param request credenciales de acceso (correo y contraseña)
     * @return token JWT generado y metadatos
     */
    LoginResponse login(LoginRequest request);
}
