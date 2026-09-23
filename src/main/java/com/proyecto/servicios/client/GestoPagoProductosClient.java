package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Cliente Feign para el servicio de catálogo de productos de GestoPago.
 * Consume el endpoint GET /sistema/service/getProductList.do.
 */
@FeignClient(name = "gestoPagoProductos", url = "${gestopago.productos.url}")
public interface GestoPagoProductosClient {

    @GetMapping("/sistema/service/getProductList.do")
    String getProductList(
            @RequestHeader("Authorization") String authorization
    );
}