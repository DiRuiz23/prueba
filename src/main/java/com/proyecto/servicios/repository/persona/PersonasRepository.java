package com.proyecto.servicios.repository.persona;

import com.proyecto.servicios.entity.persona.Personas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonasRepository extends JpaRepository<Personas, Integer>, JpaSpecificationExecutor<Personas> {
}
