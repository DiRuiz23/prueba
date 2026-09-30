package com.proyecto.servicios.config;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuración global de timeouts para los clientes Feign.
 *
 * Previene que llamadas lentas al servicio externo GestoPago
 * bloqueen hilos del servidor indefinidamente.
 *
 * Los valores son configurables desde application.properties para
 * facilitar ajustes sin necesidad de recompilar.
 */
@Configuration
public class FeignConfig {

    @Value("${gestopago.feign.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${gestopago.feign.read-timeout-ms:15000}")
    private int readTimeoutMs;

    /**
     * Opciones de timeout aplicadas a todos los clientes Feign del contexto.
     *
     * @return {@link Request.Options} con connectTimeout y readTimeout configurados.
     */
    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(connectTimeoutMs, TimeUnit.MILLISECONDS,
                readTimeoutMs, TimeUnit.MILLISECONDS, true);
    }
}
