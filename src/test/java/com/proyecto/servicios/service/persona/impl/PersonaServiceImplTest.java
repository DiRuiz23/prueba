package com.proyecto.servicios.service.persona.impl;

import com.proyecto.servicios.dto.request.persona.PersonaFiltroRequest;
import com.proyecto.servicios.dto.request.persona.PersonaIdRequest;
import com.proyecto.servicios.dto.request.persona.PersonaRequest;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.persona.PersonaResponse;
import com.proyecto.servicios.entity.persona.Personas;
import com.proyecto.servicios.exception.persona.PersonaNoEncontradaException;
import com.proyecto.servicios.repository.persona.PersonasRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: PersonaServiceImpl")
class PersonaServiceImplTest {

    @Mock
    private PersonasRepository personasRepository;

    @InjectMocks
    private PersonaServiceImpl personaService;

    private Personas persona;

    @BeforeEach
    void setUp() {
        persona = new Personas();
        persona.setId(1);
        persona.setNombre("Mario");
        persona.setApellidoPaterno("López");
        persona.setApellidoMaterno("García");
    }

    @Test
    @DisplayName("crearPersona: persiste datos correctamente")
    void crearPersona_exitoso() {
        PersonaRequest req = new PersonaRequest();
        req.setNombre("  Mario ");
        req.setApellidoPaterno(" López ");
        req.setApellidoMaterno(" García ");

        when(personasRepository.save(any(Personas.class))).thenAnswer(inv -> {
            Personas p = inv.getArgument(0);
            p.setId(1);
            return p;
        });

        PersonaResponse resp = personaService.crearPersona(req);

        assertNotNull(resp);
        assertEquals(1, resp.getId());
        assertEquals("Mario", resp.getNombre());
        assertEquals("López", resp.getApellidoPaterno());
        assertEquals("García", resp.getApellidoMaterno());
    }

    @Test
    @DisplayName("actualizarPersona: actualiza por ID existente")
    void actualizarPersona_porId_exitoso() {
        PersonaRequest req = new PersonaRequest();
        req.setId(1);
        req.setNombre("Mario Alberto");
        req.setApellidoPaterno("López");
        req.setApellidoMaterno("García");

        when(personasRepository.findById(1)).thenReturn(Optional.of(persona));
        when(personasRepository.save(any(Personas.class))).thenAnswer(inv -> inv.getArgument(0));

        PersonaResponse resp = personaService.actualizarPersona(req);

        assertNotNull(resp);
        assertEquals("Mario Alberto", resp.getNombre());
        verify(personasRepository).save(persona);
    }

    @Test
    @DisplayName("actualizarPersona: no encontrada por ID lanza PersonaNoEncontradaException")
    void actualizarPersona_noEncontrada() {
        PersonaRequest req = new PersonaRequest();
        req.setId(999);
        req.setNombre("Inexistente");
        req.setApellidoPaterno("Paterno");
        req.setApellidoMaterno("Materno");

        when(personasRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(PersonaNoEncontradaException.class, () -> personaService.actualizarPersona(req));
    }

    @Test
    @DisplayName("eliminarPersona: elimina por ID exitosamente")
    void eliminarPersona_porId_exitoso() {
        PersonaIdRequest req = new PersonaIdRequest();
        req.setId(1);

        when(personasRepository.findById(1)).thenReturn(Optional.of(persona));

        personaService.eliminarPersona(req);

        verify(personasRepository).delete(persona);
    }

    @Test
    @DisplayName("eliminarPersona: persona no existe lanza PersonaNoEncontradaException")
    void eliminarPersona_noExiste() {
        PersonaIdRequest req = new PersonaIdRequest();
        req.setId(888);

        when(personasRepository.findById(888)).thenReturn(Optional.empty());

        assertThrows(PersonaNoEncontradaException.class, () -> personaService.eliminarPersona(req));
    }

    @Test
    @DisplayName("consultar: retorna página de personas")
    void consultar_exitoso() {
        PersonaFiltroRequest req = new PersonaFiltroRequest();
        req.setNombre("Mario");

        Page<Personas> pagina = new PageImpl<>(List.of(persona));
        when(personasRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        PaginaResponse<PersonaResponse> resp = personaService.consultar(req);

        assertNotNull(resp);
        assertEquals(1, resp.getContenido().size());
        assertEquals("Mario", resp.getContenido().get(0).getNombre());
    }
}

