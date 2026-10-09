package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.banco.CorreoElectronicoDuplicadoException;
import com.proyecto.servicios.exception.banco.RecursoDuplicadoException;
import com.proyecto.servicios.exception.banco.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.banco.AgregarUsuarioRequest;
import com.proyecto.servicios.model.banco.UsuarioResponse;
import com.proyecto.servicios.repositorys.banco.ClienteRepository;
import com.proyecto.servicios.repositorys.banco.UsuarioRepository;
import com.proyecto.servicios.service.banco.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> filtrar(String correo, Boolean activo, Long clienteId) {
        List<Usuario> usuarios;

        if (clienteId != null) {
            // Filtro exacto por cliente
            usuarios = usuarioRepository.findByClienteId(clienteId)
                    .map(List::of)
                    .orElse(List.of());
        } else if (correo != null && !correo.isBlank()) {
            // Búsqueda parcial por correo
            usuarios = usuarioRepository.findByCorreoContainingIgnoreCase(correo);
        } else if (activo != null) {
            // Filtro por estado activo/inactivo
            usuarios = usuarioRepository.findByActivo(activo);
        } else {
            // Sin filtros: retornar todos
            usuarios = usuarioRepository.findAll();
        }

        return usuarios.stream()
                .map(UsuarioResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UsuarioResponse agregarUsuario(AgregarUsuarioRequest request) {
        // 1. Verificar que el cliente exista
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(
                        "No se encontró el cliente con ID: " + request.getClienteId()));

        // 2. Verificar que el cliente no tenga ya un usuario asociado
        if (usuarioRepository.findByClienteId(cliente.getId()).isPresent()) {
            throw new RecursoDuplicadoException(
                    "El cliente ID " + cliente.getId() + " ya tiene un usuario de acceso asociado.");
        }

        // 3. Verificar que el correo no esté en uso
        String correoNormalizado = request.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correoNormalizado)) {
            throw new CorreoElectronicoDuplicadoException(
                    "El correo '" + correoNormalizado + "' ya está registrado en el sistema.");
        }

        // 4. Crear y persistir el usuario
        Usuario usuario = Usuario.builder()
                .cliente(cliente)
                .correo(correoNormalizado)
                .password(passwordEncoder.encode(request.getPassword()))
                .activo(true)
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario creado para el cliente ID: {}", cliente.getId());

        return UsuarioResponse.from(usuario);
    }
}
