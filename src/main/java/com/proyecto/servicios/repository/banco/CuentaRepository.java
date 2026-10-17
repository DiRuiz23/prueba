package com.proyecto.servicios.repository.banco;

import com.proyecto.servicios.entity.banco.Cuenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long>, JpaSpecificationExecutor<Cuenta> {

    /** Incluye al cliente en la misma consulta porque la respuesta muestra su nombre. */
    @EntityGraph(attributePaths = "cliente")
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    boolean existsByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByClienteId(Long clienteId);

    /** Búsqueda dinámica paginada; el cliente se obtiene en la misma consulta (evita N+1). */
    @Override
    @NonNull
    @EntityGraph(attributePaths = "cliente")
    Page<Cuenta> findAll(Specification<Cuenta> spec, @NonNull Pageable pageable);
}
