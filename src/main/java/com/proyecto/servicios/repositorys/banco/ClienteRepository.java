package com.proyecto.servicios.repositorys.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByCorreo(String correo);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByNombreContainingIgnoreCase(String nombre);

    List<Cliente> findByApellidoPaternoContainingIgnoreCase(String apellidoPaterno);

    List<Cliente> findByApellidoMaternoContainingIgnoreCase(String apellidoMaterno);

    List<Cliente> findByFechaCreacionBetween(OffsetDateTime inicio, OffsetDateTime fin);

    @Query("SELECT c FROM Cliente c JOIN c.cuentas cu WHERE cu.numeroCuenta = :numeroCuenta")
    Optional<Cliente> findByNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);
}
