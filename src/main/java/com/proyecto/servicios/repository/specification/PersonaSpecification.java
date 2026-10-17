package com.proyecto.servicios.repository.specification;

import com.proyecto.servicios.entity.persona.Personas;
import org.springframework.data.jpa.domain.Specification;

import static com.proyecto.servicios.repository.specification.SpecificationUtils.contieneIgnorandoMayusculas;
import static com.proyecto.servicios.repository.specification.SpecificationUtils.tieneTexto;

/**
 * Criterios combinables para la búsqueda dinámica de personas.
 */
public final class PersonaSpecification {

    private PersonaSpecification() {
    }

    public static Specification<Personas> nombreContiene(String nombre) {
        return (root, query, cb) -> tieneTexto(nombre)
                ? contieneIgnorandoMayusculas(cb, root.get("nombre"), nombre) : null;
    }

    public static Specification<Personas> apellidoPaternoContiene(String apellido) {
        return (root, query, cb) -> tieneTexto(apellido)
                ? contieneIgnorandoMayusculas(cb, root.get("apellidoPaterno"), apellido) : null;
    }

    public static Specification<Personas> apellidoMaternoContiene(String apellido) {
        return (root, query, cb) -> tieneTexto(apellido)
                ? contieneIgnorandoMayusculas(cb, root.get("apellidoMaterno"), apellido) : null;
    }
}
