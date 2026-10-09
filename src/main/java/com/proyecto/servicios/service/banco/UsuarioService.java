package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.model.banco.AgregarUsuarioRequest;
import com.proyecto.servicios.model.banco.UsuarioResponse;

import java.util.List;

/**
 * Servicio de gestión de usuarios de acceso al sistema bancario.
 */
public interface UsuarioService {

    /**
     * Filtra usuarios por criterios opcionales (correo, activo, clienteId).
     * Si no se indica ningún filtro se retornan todos los usuarios.
     *
     * @param correo   filtro parcial por correo (opcional)
     * @param activo   filtro por estado activo/inactivo (opcional)
     * @param clienteId filtro exacto por ID de cliente (opcional)
     * @return lista de usuarios que cumplen los filtros
     */
    List<UsuarioResponse> filtrar(String correo, Boolean activo, Long clienteId);

    /**
     * Agrega un usuario de acceso a un cliente que aún no tiene usuario creado.
     *
     * @param request datos del nuevo usuario
     * @return UsuarioResponse del usuario creado
     */
    UsuarioResponse agregarUsuario(AgregarUsuarioRequest request);
}
