package com.proyecto.servicios.repository.specification;

import com.proyecto.servicios.entity.banco.Usuario;
import org.springframework.data.jpa.domain.Specification;

import static com.proyecto.servicios.repository.specification.SpecificationUtils.contieneIgnorandoMayusculas;
import static com.proyecto.servicios.repository.specification.SpecificationUtils.tieneTexto;

/**
 * Criterios combinables para la búsqueda dinámica de usuarios de acceso.
 */
public final class UsuarioSpecification {

    private UsuarioSpecification() {
    }

    public static Specification<Usuario> correoContiene(String correo) {
        return (root, query, cb) -> tieneTexto(correo)
                ? contieneIgnorandoMayusculas(cb, root.get("correo"), correo) : null;
    }

    public static Specification<Usuario> activoIgual(Boolean activo) {
        return (root, query, cb) -> activo != null ? cb.equal(root.get("activo"), activo) : null;
    }

    public static Specification<Usuario> clienteIgual(Long clienteId) {
        return (root, query, cb) -> clienteId != null
                ? cb.equal(root.get("cliente").get("id"), clienteId) : null;
    }
}
