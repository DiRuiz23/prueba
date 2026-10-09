package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.catalogos.CodigoPostalCatalogo;
import com.proyecto.servicios.entity.catalogos.EstadoCatalogo;
import com.proyecto.servicios.entity.catalogos.Pais;

import java.util.List;

public interface CatalogoService {
    List<Pais> obtenerTodosLosPaises();
    List<EstadoCatalogo> obtenerEstadosPorPais(Integer paisId);
    List<CodigoPostalCatalogo> obtenerCodigosPostalesPorEstado(Integer estadoId);
    List<CodigoPostalCatalogo> buscarPorCodigoPostal(String codigo);
}
