package com.proyecto.servicios.repository.catalogos;

import com.proyecto.servicios.entity.catalogos.CodigoPostalCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodigoPostalCatalogoRepository extends JpaRepository<CodigoPostalCatalogo, Integer> {
    List<CodigoPostalCatalogo> findByEstadoId(Integer estadoId);

    List<CodigoPostalCatalogo> findByCodigo(String codigo);

    boolean existsByEstadoIdAndCodigoAndMunicipioIgnoreCase(Integer estadoId, String codigo, String municipio);

    @Query("SELECT cp FROM CodigoPostalCatalogo cp JOIN FETCH cp.estado e WHERE cp.codigo = :codigo ORDER BY cp.municipio ASC")
    List<CodigoPostalCatalogo> buscarConEstadoPorCodigo(@Param("codigo") String codigo);

    @Query("SELECT cp FROM CodigoPostalCatalogo cp JOIN FETCH cp.estado e WHERE LOWER(e.nombre) = LOWER(:estado) ORDER BY cp.municipio ASC, cp.codigo ASC")
    List<CodigoPostalCatalogo> buscarConEstadoPorNombreEstado(@Param("estado") String estado);

    @Query("SELECT cp FROM CodigoPostalCatalogo cp JOIN FETCH cp.estado e ORDER BY e.nombre ASC, cp.municipio ASC, cp.codigo ASC")
    List<CodigoPostalCatalogo> buscarTodasConEstado();
}
