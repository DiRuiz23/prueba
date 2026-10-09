package com.proyecto.servicios.repositorys.banco;

import com.proyecto.servicios.entity.banco.HistorialBloqueoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialBloqueoCuentaRepository extends JpaRepository<HistorialBloqueoCuenta, Long> {
    List<HistorialBloqueoCuenta> findByCuentaIdOrderByFechaCambioDesc(Long cuentaId);
}
