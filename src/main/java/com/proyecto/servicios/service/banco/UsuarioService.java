package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.dto.request.banco.AgregarUsuarioRequest;
import com.proyecto.servicios.dto.request.banco.UsuarioFiltroRequest;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.UsuarioResponse;

public interface UsuarioService {

    /** Consulta usuarios aplicando filtros combinables y paginación. */
    PaginaResponse<UsuarioResponse> consultar(UsuarioFiltroRequest request);

    /** Agrega un usuario de acceso a un cliente que aún no tiene usuario creado. */
    UsuarioResponse agregarUsuario(AgregarUsuarioRequest request);
}
