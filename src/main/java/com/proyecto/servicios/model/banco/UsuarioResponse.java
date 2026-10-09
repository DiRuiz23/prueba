package com.proyecto.servicios.model.banco;

import com.proyecto.servicios.entity.banco.Usuario;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para Usuario. No expone la contraseña cifrada.
 */
@Data
public class UsuarioResponse {

    private Long id;
    private Long clienteId;
    private String correo;
    private Boolean activo;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;

    /**
     * Convierte una entidad Usuario en un UsuarioResponse (sin exponer password).
     *
     * @param usuario entidad persistida
     * @return DTO listo para serializar
     */
    public static UsuarioResponse from(Usuario usuario) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(usuario.getId());
        r.setCorreo(usuario.getCorreo());
        r.setActivo(usuario.getActivo());
        r.setFechaCreacion(usuario.getFechaCreacion());
        r.setFechaActualizacion(usuario.getFechaActualizacion());

        if (usuario.getCliente() != null) {
            r.setClienteId(usuario.getCliente().getId());
        }

        return r;
    }
}
