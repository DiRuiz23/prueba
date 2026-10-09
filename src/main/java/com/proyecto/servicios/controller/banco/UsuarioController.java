package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.banco.AgregarUsuarioRequest;
import com.proyecto.servicios.model.banco.UsuarioResponse;
import com.proyecto.servicios.service.banco.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios de acceso al sistema bancario")
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * GET /usuarios/filtro
     * Permite buscar usuarios aplicando filtros opcionales.
     * Si no se indica ningún filtro retorna todos los usuarios.
     *
     * @param correo    filtro parcial por correo (opcional)
     * @param activo    filtro por estado activo/inactivo (opcional)
     * @param clienteId filtro exacto por ID de cliente (opcional)
     */
    @GetMapping("/filtro")
    @Operation(summary = "Filtrar usuarios por correo, estado activo o cliente ID")
    public ResponseEntity<List<UsuarioResponse>> filtrar(
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(usuarioService.filtrar(correo, activo, clienteId));
    }

    /**
     * PUT /usuarios/agregar
     * Crea un usuario de acceso para un cliente que aún no tiene uno asociado.
     * El correo electrónico actuará como nombre de usuario y la contraseña
     * será cifrada con BCrypt.
     *
     * @param request datos del nuevo usuario (clienteId, correo, password)
     */
    @PutMapping("/agregar")
    @Operation(summary = "Agregar un usuario de acceso a un cliente que aún no tiene usuario")
    public ResponseEntity<UsuarioResponse> agregar(
            @Valid @RequestBody AgregarUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.agregarUsuario(request));
    }
}
