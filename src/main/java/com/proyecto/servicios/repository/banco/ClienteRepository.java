package com.proyecto.servicios.repository.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    /**
     * Búsqueda dinámica paginada. El domicilio (relación a uno) se obtiene en la misma consulta;
     * las cuentas se cargan por lotes gracias a {@code @BatchSize} en la entidad.
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = "domicilio")
    Page<Cliente> findAll(Specification<Cliente> spec, @NonNull Pageable pageable);
}
