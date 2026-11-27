package com.proyecto.servicios.repository.catalogos;

import com.proyecto.servicios.entity.catalogos.ColoniaCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColoniaCatalogoRepository extends JpaRepository<ColoniaCatalogo, Integer> {
    List<ColoniaCatalogo> findByCodigoPostalOrderByNombreAsc(String codigoPostal);
}
