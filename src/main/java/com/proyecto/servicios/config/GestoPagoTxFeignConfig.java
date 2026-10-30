package com.proyecto.servicios.config;

import feign.Request;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Configuración Feign específica para transacciones financieras de GestoPago (sendTx y confirmTx).
 *
 * Exige un readTimeout de al menos 65 segundos (el proveedor puede tardar hasta 62 segundos
 * en responder) y NUNCA reintenta automáticamente para prevenir dobles cobros financieros.
 */
public class GestoPagoTxFeignConfig {

    @Bean
    public Request.Options txRequestOptions(
            @Value("${gestopago.tx.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${gestopago.tx.read-timeout-ms:65000}") int readTimeout
    ) {
        return new Request.Options(connectTimeout, TimeUnit.MILLISECONDS, readTimeout, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public Retryer txRetryer() {
        return Retryer.NEVER_RETRY;
    }
}

