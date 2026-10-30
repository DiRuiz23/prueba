package com.proyecto.servicios.service.gestopago;

import com.proyecto.servicios.client.model.CatProductXml.ProductoItem;
import com.proyecto.servicios.entity.gestopago.CatProductDocument;
import com.proyecto.servicios.exception.banco.MontoInvalidoException;
import com.proyecto.servicios.exception.gestopago.CatalogoNoDisponibleException;
import com.proyecto.servicios.exception.gestopago.ProductoNoEncontradoException;
import com.proyecto.servicios.repository.gestopago.CatProductRepository;
import com.proyecto.servicios.service.banco.strategy.ReglaMontoStrategy;
import com.proyecto.servicios.service.banco.strategy.ValidacionMontoResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class CatalogoPagoValidator {

    private final CatProductRepository catProductRepository;
    private final List<ReglaMontoStrategy> strategies;
    private final int maxAntiguedadHoras;

    public CatalogoPagoValidator(
            CatProductRepository catProductRepository,
            List<ReglaMontoStrategy> strategies,
            @Value("${gestopago.catalogo.max-antiguedad-horas:48}") int maxAntiguedadHoras
    ) {
        this.catProductRepository = catProductRepository;
        this.strategies = strategies;
        this.maxAntiguedadHoras = maxAntiguedadHoras;
    }

    public ProductoItem obtenerYValidarProducto(Integer idProducto, Integer idServicio) {
        CatProductDocument doc = catProductRepository.findTopByOrderByFechaActualizacionDesc()
                .orElseThrow(() -> new CatalogoNoDisponibleException(
                        "El catálogo de productos de GestoPago no está disponible en la base de datos."
                ));

        if (doc.getFechaActualizacion() != null &&
                doc.getFechaActualizacion().isBefore(LocalDateTime.now().minusHours(maxAntiguedadHoras))) {
            log.warn("Catálogo de GestoPago desactualizado. Última sincronización: {}", doc.getFechaActualizacion());
            throw new CatalogoNoDisponibleException(
                    "El catálogo de productos de servicios se encuentra desactualizado. Intente más tarde."
            );
        }

        if (doc.getProducts() == null || doc.getProducts().isEmpty()) {
            throw new CatalogoNoDisponibleException("El catálogo de productos está vacío.");
        }

        return doc.getProducts().stream()
                .filter(p -> p.getIdProducto() != null && p.getIdProducto().equals(idProducto)
                        && p.getIdServicio() != null && p.getIdServicio().equals(idServicio))
                .findFirst()
                .orElseThrow(() -> new ProductoNoEncontradoException(String.format(
                        "El producto con ID %d y servicio %d no está disponible en el catálogo.",
                        idProducto, idServicio
                )));
    }

    public ValidacionMontoResult validarOperacion(
            Integer idProducto,
            Integer idServicio,
            BigDecimal monto,
            String telefonoReferencia
    ) {
        ProductoItem producto = obtenerYValidarProducto(idProducto, idServicio);

        ReglaMontoStrategy strategy = strategies.stream()
                .filter(s -> s.soporta(producto.getTipoFront()))
                .findFirst()
                .orElseThrow(() -> new MontoInvalidoException(
                        "Tipo de producto o servicio no soportado (tipoFront: " + producto.getTipoFront() + ")."
                ));

        strategy.validarReferencia(telefonoReferencia, producto);
        return strategy.validarYCalcular(monto, producto);
    }
}

