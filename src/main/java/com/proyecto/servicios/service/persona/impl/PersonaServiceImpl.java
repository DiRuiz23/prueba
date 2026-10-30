package com.proyecto.servicios.service.persona.impl;

import com.proyecto.servicios.dto.request.persona.PersonaFiltroRequest;
import com.proyecto.servicios.dto.request.persona.PersonaIdRequest;
import com.proyecto.servicios.dto.request.persona.PersonaRequest;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.persona.PersonaResponse;
import com.proyecto.servicios.entity.persona.Personas;
import com.proyecto.servicios.exception.persona.PersonaNoEncontradaException;
import com.proyecto.servicios.repository.persona.PersonasRepository;
import com.proyecto.servicios.repository.specification.PersonaSpecification;
import com.proyecto.servicios.service.persona.PersonaService;
import com.proyecto.servicios.util.PaginacionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonaServiceImpl implements PersonaService {

    private final PersonasRepository personasRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PersonaResponse crearPersona(PersonaRequest request) {
        Personas persona = new Personas();
        persona.setNombre(request.getNombre().trim());
        persona.setApellidoPaterno(request.getApellidoPaterno().trim());
        persona.setApellidoMaterno(request.getApellidoMaterno().trim());

        persona = personasRepository.save(persona);
        log.info("Persona creada con ID: {}", persona.getId());
        return PersonaResponse.desdeEntidad(persona);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PersonaResponse actualizarPersona(PersonaRequest request) {
        Personas persona;
        if (request.getId() != null) {
            persona = personasRepository.findById(request.getId())
                    .orElseThrow(() -> new PersonaNoEncontradaException(
                            "No se encontró la persona con ID: " + request.getId()));
        } else {
            // Si no se proporcionó ID, buscar por nombre
            persona = personasRepository.findAll(PersonaSpecification.nombreContiene(request.getNombre()))
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new PersonaNoEncontradaException(
                            "La persona con nombre '" + request.getNombre() + "' no existe"));
        }

        persona.setNombre(request.getNombre().trim());
        persona.setApellidoPaterno(request.getApellidoPaterno().trim());
        persona.setApellidoMaterno(request.getApellidoMaterno().trim());

        persona = personasRepository.save(persona);
        log.info("Persona ID: {} actualizada", persona.getId());
        return PersonaResponse.desdeEntidad(persona);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void eliminarPersona(PersonaIdRequest request) {
        Personas persona;
        if (request.getId() != null) {
            persona = personasRepository.findById(request.getId())
                    .orElseThrow(() -> new PersonaNoEncontradaException(
                            "No se encontró la persona con ID: " + request.getId()));
        } else {
            persona = personasRepository.findAll(PersonaSpecification.nombreContiene(request.getNombre()))
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new PersonaNoEncontradaException(
                            "La persona con nombre '" + request.getNombre() + "' no existe"));
        }

        personasRepository.delete(persona);
        log.info("Persona ID: {} eliminada correctamente", persona.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<PersonaResponse> consultar(PersonaFiltroRequest request) {
        Specification<Personas> spec = Specification.allOf(
                PersonaSpecification.nombreContiene(request.getNombre()),
                PersonaSpecification.apellidoPaternoContiene(request.getApellidoPaterno()),
                PersonaSpecification.apellidoMaternoContiene(request.getApellidoMaterno())
        );

        Pageable pageable = PaginacionUtils.crearPageable(
                request.getPaginacion(), request.getOrdenarPor(), "id");

        Page<Personas> pagina = personasRepository.findAll(spec, pageable);
        return PaginaResponse.desdePagina(pagina, PersonaResponse::desdeEntidad);
    }
}
