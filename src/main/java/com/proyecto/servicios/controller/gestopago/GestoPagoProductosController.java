package com.proyecto.servicios.controller.gestopago;

import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.dto.response.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.service.gestopago.GestoPagoImagenService;
import com.proyecto.servicios.service.gestopago.GestoPagoProductosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Controller REST para exponer el catálogo de productos y logotipos de GestoPago.
 */
@RestController
@Tag(name = "GestoPago", description = "Catálogo de productos de servicios y recargas")
public class GestoPagoProductosController {

    private final GestoPagoProductosService gestoPagoProductosService;
    private final GestoPagoImagenService gestoPagoImagenService;

    public GestoPagoProductosController(GestoPagoProductosService gestoPagoProductosService,
                                        GestoPagoImagenService gestoPagoImagenService) {
        this.gestoPagoProductosService = gestoPagoProductosService;
        this.gestoPagoImagenService = gestoPagoImagenService;
    }

    @GetMapping(value = "/gestopago/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtener catálogo de productos y servicios de GestoPago")
    public ResponseEntity<GenericResponse<GestoPagoProductListResponse>> obtenerProductos() {
        GestoPagoProductListResponse catalogo = gestoPagoProductosService.obtenerCatalogoProductos();
        return ResponseEntity.ok(GenericResponse.exito("Catálogo obtenido correctamente", catalogo));
    }

    @GetMapping("/gestopago/servicios/{idServicio}/imagen")
    @Operation(summary = "Obtener imagen del servicio de GestoPago")
    public ResponseEntity<byte[]> obtenerImagenServicio(@PathVariable Integer idServicio) {
        return gestoPagoImagenService.obtenerImagen(idServicio)
                .map(img -> ResponseEntity.ok()
                        .contentType(img.contentType())
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(24)).cachePublic())
                        .body(img.bytes()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

