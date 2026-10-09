package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.EstadoCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstadoCatalogoRepository extends JpaRepository<EstadoCatalogo, Integer> {
    List<EstadoCatalogo> findByPaisId(Integer paisId);
}
