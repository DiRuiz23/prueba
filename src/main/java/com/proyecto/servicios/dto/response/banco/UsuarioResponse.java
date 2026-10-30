package com.proyecto.servicios.dto.response.banco;

import com.proyecto.servicios.entity.banco.Usuario;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para Usuario. No expone la contraseña cifrada.
 */
@Data
@NoArgsConstructor
public class UsuarioResponse {

    private Long id;
    private Long clienteId;
    private String correo;
    private Boolean activo;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;

    public static UsuarioResponse desdeEntidad(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
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

    public static UsuarioResponse from(Usuario usuario) {
        return desdeEntidad(usuario);
    }
}
