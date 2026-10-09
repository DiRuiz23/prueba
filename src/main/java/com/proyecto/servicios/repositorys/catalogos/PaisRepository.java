package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.Pais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaisRepository extends JpaRepository<Pais, Integer> {
    Optional<Pais> findByCodigoIso(String codigoIso);
}
