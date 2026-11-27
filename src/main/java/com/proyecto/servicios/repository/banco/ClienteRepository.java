package com.proyecto.servicios.repository.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Cliente c
            WHERE c.telefonoMovil = :telefono
              AND c.activo = true
              AND EXISTS (
                  SELECT cta FROM Cuenta cta
                  WHERE cta.cliente = c
                    AND cta.estatus <> com.proyecto.servicios.enums.EstatusCuenta.INACTIVA
              )
            """)
    boolean existsTelefonoActivo(@Param("telefono") String telefono);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Cliente c
            WHERE c.telefonoMovil = :telefono
              AND c.id <> :clienteId
              AND c.activo = true
              AND EXISTS (
                  SELECT cta FROM Cuenta cta
                  WHERE cta.cliente = c
                    AND cta.estatus <> com.proyecto.servicios.enums.EstatusCuenta.INACTIVA
              )
            """)
    boolean existsTelefonoActivoEnOtroCliente(@Param("telefono") String telefono, @Param("clienteId") Long clienteId);

    /**
     * Búsqueda dinámica paginada. El domicilio (relación a uno) se obtiene en la misma consulta;
     * las cuentas se cargan por lotes gracias a {@code @BatchSize} en la entidad.
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = "domicilio")
    Page<Cliente> findAll(Specification<Cliente> spec, @NonNull Pageable pageable);
}
