package com.proyecto.servicios.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Configuración explícita de repositorios MongoDB.
 *
 * Es necesaria para evitar que @EnableJpaRepositories en ConfigDB intente
 * registrar los repositorios MongoDB (CatProductRepository) como repositorios JPA.
 *
 * Apunta exclusivamente al paquete de repositorios Mongo de GestoPago.
 */
@Slf4j
@Configuration
@EnableMongoRepositories(basePackages = "com.proyecto.servicios.repositorys.gestopago")
public class MongoConfig {
}
