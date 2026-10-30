package com.proyecto.servicios.repository.banco;

import com.proyecto.servicios.entity.banco.MovimientoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoCuentaRepository extends JpaRepository<MovimientoCuenta, Long> {

    List<MovimientoCuenta> findByCuentaIdOrderByFechaMovimientoDesc(Long cuentaId);

    List<MovimientoCuenta> findByTransaccionServicioId(Long transaccionServicioId);
}
