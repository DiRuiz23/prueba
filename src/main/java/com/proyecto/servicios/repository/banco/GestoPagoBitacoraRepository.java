package com.proyecto.servicios.repository.banco;

import com.proyecto.servicios.entity.banco.GestoPagoBitacora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GestoPagoBitacoraRepository extends JpaRepository<GestoPagoBitacora, Long> {

    List<GestoPagoBitacora> findByUpcOrderByFechaRegistroDesc(String upc);

    List<GestoPagoBitacora> findByTransaccionServicioIdOrderByFechaRegistroDesc(Long transaccionServicioId);
}
