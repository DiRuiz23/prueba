package com.proyecto.servicios.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * Utilidades compartidas por las Specifications de búsqueda dinámica.
 */
final class SpecificationUtils {

    private static final char ESCAPE = '\\';

    private SpecificationUtils() {
    }

    static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    /**
     * Predicado "contiene" insensible a mayúsculas. Escapa los comodines del usuario
     * ({@code %} y {@code _}) para que se busquen de forma literal.
     */
    static Predicate contieneIgnorandoMayusculas(CriteriaBuilder cb, Expression<String> campo, String valor) {
        String escapado = valor.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return cb.like(cb.lower(campo), "%" + escapado + "%", ESCAPE);
    }
}
