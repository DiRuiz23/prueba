package com.proyecto.servicios.service.banco;

import java.util.Map;

/**
 * Servicio de autenticación. Centraliza la lógica de login y generación de JWT.
 */
public interface AuthService {

    /**
     * Autentica a un usuario y genera un token JWT.
     *
     * @param correo   correo electrónico del usuario
     * @param password contraseña en texto plano (se valida contra el hash BCrypt)
     * @return mapa con token JWT, tipo y correo
     */
    Map<String, String> login(String correo, String password);
}
