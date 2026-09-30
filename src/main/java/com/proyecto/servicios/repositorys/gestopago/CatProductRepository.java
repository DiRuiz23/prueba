package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.CatProductDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio MongoDB para la colección de catálogo de productos de GestoPago.
 */
@Repository
public interface CatProductRepository extends MongoRepository<CatProductDocument, String> {

    /**
     * Retorna el documento de catálogo más reciente según su fecha de actualización.
     */
    Optional<CatProductDocument> findTopByOrderByFechaActualizacionDesc();
}
