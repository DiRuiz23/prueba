package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.config.security.JwtService;
import com.proyecto.servicios.dto.request.auth.LoginRequest;
import com.proyecto.servicios.dto.response.auth.LoginResponse;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.CredencialesInvalidasException;
import com.proyecto.servicios.exception.banco.UsuarioInactivoException;
import com.proyecto.servicios.exception.banco.UsuarioNoEncontradoException;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.service.banco.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();
        log.info("Intento de autenticación para: {}", correo);

        // 1. Verificar que el usuario exista
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(
                        "No existe un usuario registrado con el correo: " + correo));

        // 2. Verificar que el usuario esté activo
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("Intento de acceso de usuario inactivo: {}", correo);
            throw new UsuarioInactivoException(
                    "El usuario está inactivo. Comuníquese con soporte bancario.");
        }

        // 3. Verificar las credenciales
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("Credenciales inválidas para el correo: {}", correo);
            throw new CredencialesInvalidasException("Las credenciales proporcionadas son incorrectas.");
        }

        // 4. Generar token JWT
        User userDetails = new User(usuario.getCorreo(), usuario.getPassword(), Collections.emptyList());
        String token = jwtService.generarToken(userDetails);

        log.info("Autenticación exitosa para: {}", correo);
        return new LoginResponse(token, "Bearer", usuario.getCorreo());
    }
}
