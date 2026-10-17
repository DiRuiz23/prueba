package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.dto.request.banco.AgregarUsuarioRequest;
import com.proyecto.servicios.dto.request.banco.UsuarioFiltroRequest;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.UsuarioResponse;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.banco.CorreoElectronicoDuplicadoException;
import com.proyecto.servicios.exception.RecursoDuplicadoException;
import com.proyecto.servicios.repository.banco.ClienteRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.repository.specification.UsuarioSpecification;
import com.proyecto.servicios.service.banco.UsuarioService;
import com.proyecto.servicios.util.PaginacionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> consultar(UsuarioFiltroRequest request) {
        Specification<Usuario> spec = Specification.allOf(
                UsuarioSpecification.correoContiene(request.getCorreo()),
                UsuarioSpecification.activoIgual(request.getActivo()),
                UsuarioSpecification.clienteIgual(request.getClienteId())
        );

        Pageable pageable = PaginacionUtils.crearPageable(
                request.getPaginacion(), request.getOrdenarPor(), "id");

        Page<Usuario> pagina = usuarioRepository.findAll(spec, pageable);
        return PaginaResponse.desdePagina(pagina, UsuarioResponse::desdeEntidad);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UsuarioResponse agregarUsuario(AgregarUsuarioRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(
                        "No se encontró el cliente con ID: " + request.getClienteId()));

        if (usuarioRepository.existsByClienteId(cliente.getId())) {
            throw new RecursoDuplicadoException(
                    "El cliente ID " + cliente.getId() + " ya tiene un usuario de acceso asociado.");
        }

        String correoNormalizado = request.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correoNormalizado)) {
            throw new CorreoElectronicoDuplicadoException(
                    "El correo '" + correoNormalizado + "' ya está registrado en el sistema.");
        }

        Usuario usuario = Usuario.builder()
                .cliente(cliente)
                .correo(correoNormalizado)
                .password(passwordEncoder.encode(request.getPassword()))
                .activo(true)
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario creado para el cliente ID: {}", cliente.getId());

        return UsuarioResponse.desdeEntidad(usuario);
    }
}
