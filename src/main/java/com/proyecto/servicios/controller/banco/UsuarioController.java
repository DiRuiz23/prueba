package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.dto.request.banco.AgregarUsuarioRequest;
import com.proyecto.servicios.dto.request.banco.UsuarioFiltroRequest;
import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.UsuarioResponse;
import com.proyecto.servicios.service.banco.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión y consulta dinámica de usuarios de acceso al sistema bancario")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @Operation(summary = "Agregar un usuario de acceso a un cliente que aún no tiene usuario asociado")
    public ResponseEntity<GenericResponse<UsuarioResponse>> agregar(
            @Valid @RequestBody AgregarUsuarioRequest request) {
        UsuarioResponse nuevo = usuarioService.agregarUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito("Usuario de acceso creado exitosamente", nuevo));
    }

    @PostMapping("/consultar")
    @Operation(summary = "Consultar usuarios aplicando filtros combinables y paginación")
    public ResponseEntity<GenericResponse<PaginaResponse<UsuarioResponse>>> consultar(
            @Valid @RequestBody UsuarioFiltroRequest request) {
        PaginaResponse<UsuarioResponse> pagina = usuarioService.consultar(request);
        return ResponseEntity.ok(GenericResponse.exito("Consulta de usuarios realizada correctamente", pagina));
    }
}
