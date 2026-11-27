package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.catalogos.CodigoPostalCatalogo;
import com.proyecto.servicios.entity.catalogos.EstadoCatalogo;
import com.proyecto.servicios.entity.catalogos.Pais;
import com.proyecto.servicios.repository.catalogos.CodigoPostalCatalogoRepository;
import com.proyecto.servicios.repository.catalogos.EstadoCatalogoRepository;
import com.proyecto.servicios.repository.catalogos.PaisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: CatalogoServiceImpl")
class CatalogoServiceImplTest {

    @Mock
    private PaisRepository paisRepository;

    @Mock
    private EstadoCatalogoRepository estadoCatalogoRepository;

    @Mock
    private CodigoPostalCatalogoRepository codigoPostalCatalogoRepository;

    @InjectMocks
    private CatalogoServiceImpl catalogoService;

    @Test
    @DisplayName("obtenerTodosLosPaises: delega en repository")
    void obtenerTodosLosPaises() {
        Pais pais = new Pais();
        pais.setId(1);
        pais.setNombre("México");
        when(paisRepository.findAll()).thenReturn(List.of(pais));

        List<Pais> resultado = catalogoService.obtenerTodosLosPaises();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("México", resultado.get(0).getNombre());
        verify(paisRepository).findAll();
    }

    @Test
    @DisplayName("obtenerEstadosPorPais: delega en repository")
    void obtenerEstadosPorPais() {
        EstadoCatalogo estado = new EstadoCatalogo();
        estado.setId(9);
        estado.setNombre("CDMX");
        when(estadoCatalogoRepository.findByPaisId(1)).thenReturn(List.of(estado));

        List<EstadoCatalogo> resultado = catalogoService.obtenerEstadosPorPais(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("CDMX", resultado.get(0).getNombre());
        verify(estadoCatalogoRepository).findByPaisId(1);
    }

    @Test
    @DisplayName("obtenerCodigosPostalesPorEstado: delega en repository")
    void obtenerCodigosPostalesPorEstado() {
        CodigoPostalCatalogo cp = new CodigoPostalCatalogo();
        cp.setId(100);
        cp.setCodigo("06600");
        when(codigoPostalCatalogoRepository.findByEstadoId(9)).thenReturn(List.of(cp));

        List<CodigoPostalCatalogo> resultado = catalogoService.obtenerCodigosPostalesPorEstado(9);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("06600", resultado.get(0).getCodigo());
        verify(codigoPostalCatalogoRepository).findByEstadoId(9);
    }

    @Test
    @DisplayName("buscarPorCodigoPostal: delega en repository")
    void buscarPorCodigoPostal() {
        CodigoPostalCatalogo cp = new CodigoPostalCatalogo();
        cp.setId(100);
        cp.setCodigo("06600");
        when(codigoPostalCatalogoRepository.findByCodigo("06600")).thenReturn(List.of(cp));

        List<CodigoPostalCatalogo> resultado = catalogoService.buscarPorCodigoPostal("06600");

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("06600", resultado.get(0).getCodigo());
        verify(codigoPostalCatalogoRepository).findByCodigo("06600");
    }
}

