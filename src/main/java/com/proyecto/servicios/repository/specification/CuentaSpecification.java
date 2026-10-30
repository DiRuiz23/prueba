package com.proyecto.servicios.repository.specification;

import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static com.proyecto.servicios.repository.specification.SpecificationUtils.tieneTexto;

/**
 * Criterios combinables para la búsqueda dinámica de cuentas bancarias.
 */
public final class CuentaSpecification {

    private CuentaSpecification() {
    }

    public static Specification<Cuenta> numeroCuentaIgual(String numeroCuenta) {
        return (root, query, cb) -> tieneTexto(numeroCuenta)
                ? cb.equal(root.get("numeroCuenta"), numeroCuenta.trim()) : null;
    }

    public static Specification<Cuenta> clienteIgual(Long clienteId) {
        return (root, query, cb) -> clienteId != null
                ? cb.equal(root.get("cliente").get("id"), clienteId) : null;
    }

    public static Specification<Cuenta> estatusIgual(EstatusCuenta estatus) {
        return (root, query, cb) -> estatus != null ? cb.equal(root.get("estatus"), estatus) : null;
    }

    public static Specification<Cuenta> saldoMinimo(BigDecimal saldoMinimo) {
        return (root, query, cb) -> saldoMinimo != null
                ? cb.greaterThanOrEqualTo(root.get("saldo"), saldoMinimo) : null;
    }

    public static Specification<Cuenta> saldoMaximo(BigDecimal saldoMaximo) {
        return (root, query, cb) -> saldoMaximo != null
                ? cb.lessThanOrEqualTo(root.get("saldo"), saldoMaximo) : null;
    }
}
