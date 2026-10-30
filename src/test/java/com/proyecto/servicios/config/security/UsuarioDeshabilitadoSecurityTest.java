package com.proyecto.servicios.config.security;

import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifica que un usuario dado de baja no conserve acceso aunque su JWT siga vigente. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Seguridad: usuarios dados de baja")
class UsuarioDeshabilitadoSecurityTest {

    private static final String CORREO = "titular@banco.com";

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails userDetails(boolean deshabilitado) {
        return User.builder().username(CORREO).password("hash")
                .authorities(Collections.emptyList()).disabled(deshabilitado).build();
    }

    private MockHttpServletRequest requestConToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-vigente");
        return request;
    }

    @Test
    @DisplayName("JWT vigente + usuario deshabilitado → NO queda autenticado")
    void usuarioDeshabilitado_noAutentica() throws Exception {
        when(jwtService.extraerUsername("token-vigente")).thenReturn(CORREO);
        when(userDetailsService.loadUserByUsername(CORREO)).thenReturn(userDetails(true));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestConToken(), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).esTokenValido(any(), any());
        assertNotNull(chain.getRequest(), "La cadena debe continuar; Spring Security rechaza la petición");
    }

    @Test
    @DisplayName("JWT vigente + usuario activo → queda autenticado")
    void usuarioActivo_autentica() throws Exception {
        UserDetails details = userDetails(false);
        when(jwtService.extraerUsername("token-vigente")).thenReturn(CORREO);
        when(userDetailsService.loadUserByUsername(CORREO)).thenReturn(details);
        when(jwtService.esTokenValido("token-vigente", details)).thenReturn(true);

        filter.doFilter(requestConToken(), new MockHttpServletResponse(), new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("CustomUserDetailsService marca como deshabilitado al usuario con activo = false")
    void userDetailsService_reflejaEstadoActivo() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        CustomUserDetailsService service = new CustomUserDetailsService(repo);

        when(repo.findByCorreo(CORREO)).thenReturn(Optional.of(
                Usuario.builder().correo(CORREO).password("hash").activo(false).build()));
        assertFalse(service.loadUserByUsername(CORREO).isEnabled());

        when(repo.findByCorreo(CORREO)).thenReturn(Optional.of(
                Usuario.builder().correo(CORREO).password("hash").activo(true).build()));
        assertTrue(service.loadUserByUsername(CORREO).isEnabled());
    }
}
