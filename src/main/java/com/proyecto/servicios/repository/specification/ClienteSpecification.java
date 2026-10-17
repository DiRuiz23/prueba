package com.proyecto.servicios.repository.specification;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

import static com.proyecto.servicios.repository.specification.SpecificationUtils.contieneIgnorandoMayusculas;
import static com.proyecto.servicios.repository.specification.SpecificationUtils.tieneTexto;

/**
 * Criterios combinables para la búsqueda dinámica de clientes.
 * Cada criterio se ignora (predicado {@code null}) cuando su valor no fue proporcionado,
 * por lo que pueden combinarse libremente con {@link Specification#allOf}.
 */
public final class ClienteSpecification {

    private ClienteSpecification() {
    }

    public static Specification<Cliente> nombreContiene(String nombre) {
        return (root, query, cb) -> tieneTexto(nombre)
                ? contieneIgnorandoMayusculas(cb, root.get("nombre"), nombre) : null;
    }

    public static Specification<Cliente> apellidoPaternoContiene(String apellido) {
        return (root, query, cb) -> tieneTexto(apellido)
                ? contieneIgnorandoMayusculas(cb, root.get("apellidoPaterno"), apellido) : null;
    }

    public static Specification<Cliente> apellidoMaternoContiene(String apellido) {
        return (root, query, cb) -> tieneTexto(apellido)
                ? contieneIgnorandoMayusculas(cb, root.get("apellidoMaterno"), apellido) : null;
    }

    public static Specification<Cliente> curpContiene(String curp) {
        return (root, query, cb) -> tieneTexto(curp)
                ? contieneIgnorandoMayusculas(cb, root.get("curp"), curp) : null;
    }

    public static Specification<Cliente> rfcContiene(String rfc) {
        return (root, query, cb) -> tieneTexto(rfc)
                ? contieneIgnorandoMayusculas(cb, root.get("rfc"), rfc) : null;
    }

    public static Specification<Cliente> correoContiene(String correo) {
        return (root, query, cb) -> tieneTexto(correo)
                ? contieneIgnorandoMayusculas(cb, root.get("correo"), correo) : null;
    }

    public static Specification<Cliente> activoIgual(Boolean activo) {
        return (root, query, cb) -> activo != null ? cb.equal(root.get("activo"), activo) : null;
    }

    /** Clientes que poseen la cuenta indicada (subconsulta: no duplica filas ni afecta la paginación). */
    public static Specification<Cliente> tieneNumeroCuenta(String numeroCuenta) {
        return (root, query, cb) -> {
            if (!tieneTexto(numeroCuenta)) {
                return null;
            }
            Subquery<Long> subconsulta = query.subquery(Long.class);
            Root<Cuenta> cuenta = subconsulta.from(Cuenta.class);
            subconsulta.select(cuenta.get("cliente").get("id"))
                    .where(cb.equal(cuenta.get("numeroCuenta"), numeroCuenta.trim()));
            return root.get("id").in(subconsulta);
        };
    }

    public static Specification<Cliente> creadoDesde(OffsetDateTime fechaInicio) {
        return (root, query, cb) -> fechaInicio != null
                ? cb.greaterThanOrEqualTo(root.get("fechaCreacion"), fechaInicio) : null;
    }

    public static Specification<Cliente> creadoHasta(OffsetDateTime fechaFin) {
        return (root, query, cb) -> fechaFin != null
                ? cb.lessThanOrEqualTo(root.get("fechaCreacion"), fechaFin) : null;
    }
}
