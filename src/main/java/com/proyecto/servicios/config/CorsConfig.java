package com.proyecto.servicios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuración global de CORS para la aplicación.
 *
 * Permite que Swagger UI (y otros clientes web) puedan realizar
 * peticiones al API desde el navegador sin ser bloqueadas.
 *
 * En producción se debe restringir {@code allowedOriginPatterns} a los
 * dominios específicos autorizados.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos — en producción reemplazar por dominios específicos
        config.setAllowedOriginPatterns(List.of("*"));

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Headers permitidos en la petición
        config.setAllowedHeaders(List.of("*"));

        // Headers expuestos en la respuesta
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        // Permitir cookies / credenciales
        config.setAllowCredentials(true);

        // Cache del preflight OPTIONS (en segundos)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
