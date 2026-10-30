package com.proyecto.servicios.repository.catalogos;

import com.proyecto.servicios.entity.catalogos.EstadoCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoCatalogoRepository extends JpaRepository<EstadoCatalogo, Integer> {
    List<EstadoCatalogo> findByPaisId(Integer paisId);

    Optional<EstadoCatalogo> findByPaisIdAndNombreIgnoreCase(Integer paisId, String nombre);
}
