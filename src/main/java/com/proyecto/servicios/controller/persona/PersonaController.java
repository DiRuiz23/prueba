package com.proyecto.servicios.controller.persona;

import com.proyecto.servicios.dto.request.persona.PersonaFiltroRequest;
import com.proyecto.servicios.dto.request.persona.PersonaIdRequest;
import com.proyecto.servicios.dto.request.persona.PersonaRequest;
import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.persona.PersonaResponse;
import com.proyecto.servicios.service.persona.PersonaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/personas")
@RequiredArgsConstructor
@Tag(name = "Personas", description = "Operaciones CRUD para personas")
public class PersonaController {

    private final PersonaService personaService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear una nueva persona")
    public ResponseEntity<GenericResponse<PersonaResponse>> crear(
            @Valid @RequestBody PersonaRequest request) {
        PersonaResponse creado = personaService.crearPersona(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito("Persona creada exitosamente", creado));
    }

    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar información de una persona")
    public ResponseEntity<GenericResponse<PersonaResponse>> actualizar(
            @Valid @RequestBody PersonaRequest request) {
        PersonaResponse actualizado = personaService.actualizarPersona(request);
        return ResponseEntity.ok(GenericResponse.exito("Persona actualizada correctamente", actualizado));
    }

    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Eliminar una persona (identificador enviado en el cuerpo)")
    public ResponseEntity<GenericResponse<Void>> eliminar(
            @Valid @RequestBody PersonaIdRequest request) {
        personaService.eliminarPersona(request);
        return ResponseEntity.ok(GenericResponse.exitoSinDatos("Persona eliminada correctamente"));
    }

    @PostMapping(value = "/consultar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar personas aplicando filtros dinámicos y paginación")
    public ResponseEntity<GenericResponse<PaginaResponse<PersonaResponse>>> consultar(
            @Valid @RequestBody PersonaFiltroRequest request) {
        PaginaResponse<PersonaResponse> pagina = personaService.consultar(request);
        return ResponseEntity.ok(GenericResponse.exito("Consulta de personas realizada correctamente", pagina));
    }
}
