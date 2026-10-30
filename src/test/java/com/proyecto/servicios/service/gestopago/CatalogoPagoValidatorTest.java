package com.proyecto.servicios.service.gestopago;

import com.proyecto.servicios.client.model.CatProductXml.ProductoItem;
import com.proyecto.servicios.entity.gestopago.CatProductDocument;
import com.proyecto.servicios.exception.gestopago.CatalogoNoDisponibleException;
import com.proyecto.servicios.exception.gestopago.ProductoNoEncontradoException;
import com.proyecto.servicios.repository.gestopago.CatProductRepository;
import com.proyecto.servicios.service.banco.strategy.PagoServicioStrategy;
import com.proyecto.servicios.service.banco.strategy.RecargaStrategy;
import com.proyecto.servicios.service.banco.strategy.ReglaMontoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogoPagoValidator")
class CatalogoPagoValidatorTest {

    @Mock
    private CatProductRepository catProductRepository;

    private CatalogoPagoValidator validator;

    @BeforeEach
    void setUp() {
        List<ReglaMontoStrategy> strategies = List.of(
                new RecargaStrategy(),
                new PagoServicioStrategy(new BigDecimal("5000.00"))
        );
        validator = new CatalogoPagoValidator(catProductRepository, strategies, 48);
    }

    @Test
    @DisplayName("Catálogo ausente en base de datos → CatalogoNoDisponibleException")
    void catalogoAusente_lanzaExcepcion() {
        when(catProductRepository.findTopByOrderByFechaActualizacionDesc()).thenReturn(Optional.empty());

        assertThrows(CatalogoNoDisponibleException.class,
                () -> validator.obtenerYValidarProducto(101, 202));
    }

    @Test
    @DisplayName("Catálogo desactualizado (> 48h) → CatalogoNoDisponibleException")
    void catalogoDesactualizado_lanzaExcepcion() {
        CatProductDocument doc = new CatProductDocument();
        doc.setFechaActualizacion(LocalDateTime.now().minusHours(50));
        when(catProductRepository.findTopByOrderByFechaActualizacionDesc()).thenReturn(Optional.of(doc));

        assertThrows(CatalogoNoDisponibleException.class,
                () -> validator.obtenerYValidarProducto(101, 202));
    }

    @Test
    @DisplayName("Catálogo sin productos → CatalogoNoDisponibleException")
    void catalogoVacio_lanzaExcepcion() {
        CatProductDocument doc = new CatProductDocument();
        doc.setFechaActualizacion(LocalDateTime.now());
        doc.setProducts(List.of());
        when(catProductRepository.findTopByOrderByFechaActualizacionDesc()).thenReturn(Optional.of(doc));

        assertThrows(CatalogoNoDisponibleException.class,
                () -> validator.obtenerYValidarProducto(101, 202));
    }

    @Test
    @DisplayName("Producto inexistente en catálogo vigente → ProductoNoEncontradoException")
    void productoInexistente_lanzaExcepcion() {
        ProductoItem otroItem = new ProductoItem();
        otroItem.setIdProducto(999);
        otroItem.setIdServicio(888);

        CatProductDocument doc = new CatProductDocument();
        doc.setFechaActualizacion(LocalDateTime.now());
        doc.setProducts(List.of(otroItem));
        when(catProductRepository.findTopByOrderByFechaActualizacionDesc()).thenReturn(Optional.of(doc));

        assertThrows(ProductoNoEncontradoException.class,
                () -> validator.obtenerYValidarProducto(101, 202));
    }

    @Test
    @DisplayName("Producto encontrado y vigente → se obtiene exitosamente")
    void productoValido_seObtiene() {
        ProductoItem item = new ProductoItem();
        item.setIdProducto(101);
        item.setIdServicio(202);
        item.setTipoFront(1);
        item.setPrecio("20.00");

        CatProductDocument doc = new CatProductDocument();
        doc.setFechaActualizacion(LocalDateTime.now());
        doc.setProducts(List.of(item));
        when(catProductRepository.findTopByOrderByFechaActualizacionDesc()).thenReturn(Optional.of(doc));

        ProductoItem res = validator.obtenerYValidarProducto(101, 202);
        assertSame(item, res);
    }
}
