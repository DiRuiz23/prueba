package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.CodigoPostalCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodigoPostalCatalogoRepository extends JpaRepository<CodigoPostalCatalogo, Integer> {
    List<CodigoPostalCatalogo> findByEstadoId(Integer estadoId);
    List<CodigoPostalCatalogo> findByCodigo(String codigo);
}
