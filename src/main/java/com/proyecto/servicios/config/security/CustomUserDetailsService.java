package com.proyecto.servicios.config.security;

import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + username));
                
        // Un usuario dado de baja (activo = false) queda deshabilitado: su JWT aún vigente deja de ser válido.
        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getPassword())
                .authorities(Collections.emptyList())
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }
}
