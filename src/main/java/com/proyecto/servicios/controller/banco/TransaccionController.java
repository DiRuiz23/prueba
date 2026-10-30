package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.dto.request.banco.PagoServicioRequest;
import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.dto.response.banco.PagoServicioResponse;
import com.proyecto.servicios.exception.NoAutenticadoException;
import com.proyecto.servicios.service.banco.PagoServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transacciones")
@RequiredArgsConstructor
@Tag(name = "Transacciones Bancarias", description = "Operaciones de pago de servicios y recargas electrónicas con débito a cuenta bancaria")
public class TransaccionController {

    private final PagoServicioService pagoServicioService;

    @PostMapping("/pago-servicio")
    @Operation(
            summary = "Realizar pago de servicio o recarga de tiempo aire con débito a cuenta bancaria",
            description = "Valida identidad del titular, saldo atómico y PIN transaccional, envía operación cifrada con AES a GestoPago sendTx y genera comprobante."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transacción procesada exitosamente"),
            @ApiResponse(responseCode = "202", description = "Resultado incierto (timeout/código 82); operación en conciliación"),
            @ApiResponse(responseCode = "400", description = "Parámetros de petición inválidos o faltantes"),
            @ApiResponse(responseCode = "401", description = "Token JWT faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "PIN transaccional incorrecto"),
            @ApiResponse(responseCode = "404", description = "Cuenta o producto de catálogo no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto por clave de idempotencia duplicada o en proceso"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente o monto inválido"),
            @ApiResponse(responseCode = "423", description = "PIN transaccional bloqueado temporalmente por rebasar intentos"),
            @ApiResponse(responseCode = "502", description = "Rechazo definitivo por parte de la pasarela de pagos (saldo reembolsado)"),
            @ApiResponse(responseCode = "503", description = "Servicio o pasarela de pagos no disponible")
    })
    public ResponseEntity<GenericResponse<PagoServicioResponse>> pagarServicio(
            @Valid @RequestBody PagoServicioRequest request,
            @Parameter(description = "Clave de idempotencia única (UUID) obligatoria para evitar cargos duplicados por reintentos de red", required = true)
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new NoAutenticadoException("Se requiere una sesión autenticada válida para operar.");
        }
        PagoServicioResponse respuesta = pagoServicioService.realizarPagoServicio(
                request, authentication.getName(), idempotencyKey
        );
        return ResponseEntity.ok(GenericResponse.exito("Transacción realizada exitosamente", respuesta));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar estado de una transacción",
            description = "Permite verificar el estado final de una transacción (EXITOSA, EN_PROCESO, FALLIDA, REVERSADA)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta realizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada o no pertenece al usuario autenticado")
    })
    public ResponseEntity<GenericResponse<PagoServicioResponse>> consultarTransaccion(
            @PathVariable("id") Long id,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new NoAutenticadoException("Se requiere una sesión autenticada válida para operar.");
        }
        PagoServicioResponse respuesta = pagoServicioService.consultarEstadoTransaccion(id, authentication.getName());
        return ResponseEntity.ok(GenericResponse.exito("Consulta de transacción exitosa", respuesta));
    }
}
