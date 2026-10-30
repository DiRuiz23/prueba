package com.proyecto.servicios.repository.banco;

import com.proyecto.servicios.entity.banco.Cuenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    /**
     * Débito atómico con validación condicional de saldo suficiente y cuenta activa.
     * Retorna 1 si se aplicó el débito, 0 si los fondos son insuficientes o la cuenta no está activa.
     */
    @Modifying
    @Query("UPDATE Cuenta c SET c.saldo = c.saldo - :monto, c.fechaActualizacion = CURRENT_TIMESTAMP " +
           "WHERE c.id = :id AND c.estatus = com.proyecto.servicios.enums.EstatusCuenta.ACTIVA AND c.saldo >= :monto")
    int debitarSaldoAtomico(@Param("id") Long id, @Param("monto") BigDecimal monto);

    /**
     * Reembolso o abono simétrico atómico a la cuenta.
     */
    @Modifying
    @Query("UPDATE Cuenta c SET c.saldo = c.saldo + :monto, c.fechaActualizacion = CURRENT_TIMESTAMP " +
           "WHERE c.id = :id AND c.estatus = com.proyecto.servicios.enums.EstatusCuenta.ACTIVA")
    int reembolsarSaldoAtomico(@Param("id") Long id, @Param("monto") BigDecimal monto);
}
