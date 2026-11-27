package com.proyecto.servicios.repository.catalogos;

import com.proyecto.servicios.entity.catalogos.OcupacionCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcupacionCatalogoRepository extends JpaRepository<OcupacionCatalogo, Integer> {
    List<OcupacionCatalogo> findAllByOrderByOrdenAscNombreAsc();
}
