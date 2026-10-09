package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.catalogos.CodigoPostalCatalogo;
import com.proyecto.servicios.entity.catalogos.EstadoCatalogo;
import com.proyecto.servicios.entity.catalogos.Pais;
import com.proyecto.servicios.repositorys.catalogos.CodigoPostalCatalogoRepository;
import com.proyecto.servicios.repositorys.catalogos.EstadoCatalogoRepository;
import com.proyecto.servicios.repositorys.catalogos.PaisRepository;
import com.proyecto.servicios.service.banco.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private final PaisRepository paisRepository;
    private final EstadoCatalogoRepository estadoCatalogoRepository;
    private final CodigoPostalCatalogoRepository codigoPostalCatalogoRepository;

    @Override
    @Cacheable("paises")
    public List<Pais> obtenerTodosLosPaises() {
        return paisRepository.findAll();
    }

    @Override
    @Cacheable(value = "estados", key = "#paisId")
    public List<EstadoCatalogo> obtenerEstadosPorPais(Integer paisId) {
        return estadoCatalogoRepository.findByPaisId(paisId);
    }

    @Override
    @Cacheable(value = "codigos_postales_estado", key = "#estadoId")
    public List<CodigoPostalCatalogo> obtenerCodigosPostalesPorEstado(Integer estadoId) {
        return codigoPostalCatalogoRepository.findByEstadoId(estadoId);
    }

    @Override
    @Cacheable(value = "codigos_postales", key = "#codigo")
    public List<CodigoPostalCatalogo> buscarPorCodigoPostal(String codigo) {
        return codigoPostalCatalogoRepository.findByCodigo(codigo);
    }
}
