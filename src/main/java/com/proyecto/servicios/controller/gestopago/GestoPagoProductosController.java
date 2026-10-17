package com.proyecto.servicios.controller.gestopago;

import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.dto.response.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.service.gestopago.GestoPagoProductosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para exponer el catálogo de productos de GestoPago.
 */
@RestController
@Tag(name = "GestoPago", description = "Catálogo de productos de servicios y recargas")
public class GestoPagoProductosController {

    private final GestoPagoProductosService gestoPagoProductosService;

    public GestoPagoProductosController(GestoPagoProductosService gestoPagoProductosService) {
        this.gestoPagoProductosService = gestoPagoProductosService;
    }

    @GetMapping(value = "/gestopago/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtener catálogo de productos y servicios de GestoPago")
    public ResponseEntity<GenericResponse<GestoPagoProductListResponse>> obtenerProductos() {
        GestoPagoProductListResponse catalogo = gestoPagoProductosService.obtenerCatalogoProductos();
        return ResponseEntity.ok(GenericResponse.exito("Catálogo obtenido correctamente", catalogo));
    }
}

