package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoTxFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente Feign para los endpoints transaccionales de GestoPago (sendTx y confirmTx).
 * Utiliza timeout extendido (65s) y deshabilita reintentos automáticos a ciegas.
 */
@FeignClient(name = "gestoPagoTx", url = "${gestopago.tx.url}", configuration = GestoPagoTxFeignConfig.class)
public interface GestoPagoTxClient {

    @PostMapping(value = "/sistema/service/sendTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String sendTx(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam("random") String random,
            @RequestParam("signed") String signed
    );

    @PostMapping(value = "/sistema/service/confirmTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String confirmTx(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam("random") String random,
            @RequestParam("signed") String signed
    );
}
