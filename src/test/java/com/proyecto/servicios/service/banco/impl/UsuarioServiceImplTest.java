package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.dto.request.banco.AgregarUsuarioRequest;
import com.proyecto.servicios.dto.request.banco.UsuarioFiltroRequest;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.UsuarioResponse;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.RecursoDuplicadoException;
import com.proyecto.servicios.exception.banco.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.banco.CorreoElectronicoDuplicadoException;
import com.proyecto.servicios.repository.banco.ClienteRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: UsuarioServiceImpl")
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Cliente cliente;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder().id(10L).nombre("Carlos").correo("carlos@banco.com").activo(true).build();
        usuario = Usuario.builder().id(1L).cliente(cliente).correo("carlos@banco.com").password("encodedPass").activo(true).build();
    }

    @Test
    @DisplayName("agregarUsuario: flujo exitoso")
    void agregarUsuario_exitoso() {
        AgregarUsuarioRequest req = new AgregarUsuarioRequest(10L, "  Carlos@Banco.com  ", "PlainPass123");

        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.existsByClienteId(10L)).thenReturn(false);
        when(usuarioRepository.existsByCorreo("carlos@banco.com")).thenReturn(false);
        when(passwordEncoder.encode("PlainPass123")).thenReturn("encodedPass");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UsuarioResponse resp = usuarioService.agregarUsuario(req);

        assertNotNull(resp);
        assertEquals("carlos@banco.com", resp.getCorreo());
        assertTrue(resp.getActivo());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("agregarUsuario: cliente inexistente lanza ClienteNoEncontradoException")
    void agregarUsuario_clienteNoExiste() {
        AgregarUsuarioRequest req = new AgregarUsuarioRequest(999L, "correo@test.com", "pass");
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> usuarioService.agregarUsuario(req));
    }

    @Test
    @DisplayName("agregarUsuario: cliente ya tiene usuario asignado lanza RecursoDuplicadoException")
    void agregarUsuario_clienteYaTieneUsuario() {
        AgregarUsuarioRequest req = new AgregarUsuarioRequest(10L, "correo@test.com", "pass");
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.existsByClienteId(10L)).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> usuarioService.agregarUsuario(req));
    }

    @Test
    @DisplayName("agregarUsuario: correo duplicado lanza CorreoElectronicoDuplicadoException")
    void agregarUsuario_correoDuplicado() {
        AgregarUsuarioRequest req = new AgregarUsuarioRequest(10L, "carlos@banco.com", "pass");
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.existsByClienteId(10L)).thenReturn(false);
        when(usuarioRepository.existsByCorreo("carlos@banco.com")).thenReturn(true);

        assertThrows(CorreoElectronicoDuplicadoException.class, () -> usuarioService.agregarUsuario(req));
    }

    @Test
    @DisplayName("consultar: retorna página de usuarios con filtros")
    void consultar_exitoso() {
        UsuarioFiltroRequest req = new UsuarioFiltroRequest();
        req.setCorreo("carlos");
        req.setActivo(true);

        Page<Usuario> page = new PageImpl<>(List.of(usuario));
        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PaginaResponse<UsuarioResponse> resultado = usuarioService.consultar(req);

        assertNotNull(resultado);
        assertEquals(1, resultado.getContenido().size());
        assertEquals("carlos@banco.com", resultado.getContenido().get(0).getCorreo());
    }
}

