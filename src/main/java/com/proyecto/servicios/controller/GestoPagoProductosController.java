package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.service.GestoPagoProductosService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para exponer el catálogo de productos de GestoPago.
 */
@RestController
public class GestoPagoProductosController {

    private final GestoPagoProductosService gestoPagoProductosService;

    public GestoPagoProductosController(GestoPagoProductosService gestoPagoProductosService) {
        this.gestoPagoProductosService = gestoPagoProductosService;
    }

    @GetMapping(value = "/gestopago/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GestoPagoProductListResponse> obtenerProductos() {
        return new ResponseEntity<>(gestoPagoProductosService.obtenerCatalogoProductos(), HttpStatus.OK);
    }
}

