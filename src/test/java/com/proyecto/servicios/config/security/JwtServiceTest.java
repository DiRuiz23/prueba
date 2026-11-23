package com.proyecto.servicios.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Seguridad: JwtService (Firma, claims, JTI y expiración)")
class JwtServiceTest {

    private JwtService jwtService;
    private final String secretKey = "1234567890123456789012345678901234567890123456789012345678901234"; // 64 bytes

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hora
    }

    @Test
    @DisplayName("generarToken y extraerUsername: valida subject correctamente")
    void generarToken_validaUsername() {
        UserDetails userDetails = new User("usuario@test.com", "pass", Collections.emptyList());

        String token = jwtService.generarToken(userDetails);

        assertNotNull(token);
        assertEquals("usuario@test.com", jwtService.extraerUsername(token));
        assertTrue(jwtService.esTokenValido(token, userDetails));
    }

    @Test
    @DisplayName("generarToken con JTI: almacena y extrae claim 'jti'")
    void generarToken_conJti() {
        UserDetails userDetails = new User("usuario@test.com", "pass", Collections.emptyList());

        String token = jwtService.generarToken(userDetails, "jti-uuid-456");

        assertNotNull(token);
        assertEquals("jti-uuid-456", jwtService.extraerJti(token));
        assertEquals("usuario@test.com", jwtService.extraerUsername(token));
    }

    @Test
    @DisplayName("esTokenValido: retorna false si el username difiere")
    void esTokenValido_usernameDiferente() {
        UserDetails userDetails = new User("usuario@test.com", "pass", Collections.emptyList());
        UserDetails otroUsuario = new User("otro@test.com", "pass", Collections.emptyList());

        String token = jwtService.generarToken(userDetails);

        assertFalse(jwtService.esTokenValido(token, otroUsuario));
    }
}

