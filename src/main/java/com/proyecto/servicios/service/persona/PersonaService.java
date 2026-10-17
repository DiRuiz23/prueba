package com.proyecto.servicios.service.persona;

import com.proyecto.servicios.dto.request.persona.PersonaFiltroRequest;
import com.proyecto.servicios.dto.request.persona.PersonaIdRequest;
import com.proyecto.servicios.dto.request.persona.PersonaRequest;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.persona.PersonaResponse;

public interface PersonaService {

    PersonaResponse crearPersona(PersonaRequest request);

    PersonaResponse actualizarPersona(PersonaRequest request);

    void eliminarPersona(PersonaIdRequest request);

    PaginaResponse<PersonaResponse> consultar(PersonaFiltroRequest request);
}
