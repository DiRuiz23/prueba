package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.dto.request.banco.*;
import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.CuentaResponse;
import com.proyecto.servicios.dto.response.banco.SaldoResponse;
import com.proyecto.servicios.service.banco.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas Bancarias", description = "Gestión de cuentas, consulta dinámica, saldo y cambio de estatus")
public class CuentaController {

    private final CuentaService cuentaService;

    @PostMapping
    @Operation(summary = "Crear una cuenta bancaria adicional para un cliente activo existente")
    public ResponseEntity<GenericResponse<CuentaResponse>> crearCuenta(
            @Valid @RequestBody CrearCuentaRequest request) {
        CuentaResponse respuesta = cuentaService.crearCuentaAdicional(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito("Cuenta bancaria creada exitosamente", respuesta));
    }

    @PostMapping("/consultar")
    @Operation(summary = "Consultar cuentas bancarias aplicando filtros combinables y paginación")
    public ResponseEntity<GenericResponse<PaginaResponse<CuentaResponse>>> consultarCuentas(
            @Valid @RequestBody CuentaFiltroRequest request) {
        PaginaResponse<CuentaResponse> pagina = cuentaService.consultar(request);
        return ResponseEntity.ok(GenericResponse.exito("Consulta de cuentas realizada correctamente", pagina));
    }

    @PostMapping("/detalle")
    @Operation(summary = "Consultar cuenta bancaria por su número único (enviado en el cuerpo)")
    public ResponseEntity<GenericResponse<CuentaResponse>> consultarPorNumero(
            @Valid @RequestBody NumeroCuentaRequest request) {
        CuentaResponse cuenta = cuentaService.consultarPorNumeroCuenta(request);
        return ResponseEntity.ok(GenericResponse.exito("Cuenta encontrada", cuenta));
    }

    @PostMapping("/saldo")
    @Operation(summary = "Consultar saldo disponible de una cuenta (número enviado en el cuerpo)")
    public ResponseEntity<GenericResponse<SaldoResponse>> consultarSaldo(
            @Valid @RequestBody NumeroCuentaRequest request) {
        SaldoResponse saldo = cuentaService.consultarSaldo(request);
        return ResponseEntity.ok(GenericResponse.exito("Saldo consultado correctamente", saldo));
    }

    @PatchMapping("/estatus")
    @Operation(summary = "Actualizar el estatus de una cuenta bancaria (ACTIVA, BLOQUEADA, INACTIVA)")
    public ResponseEntity<GenericResponse<CuentaResponse>> cambiarEstatus(
            @Valid @RequestBody ActualizarEstatusCuentaRequest request) {
        CuentaResponse cuenta = cuentaService.cambiarEstatusCuenta(request);
        return ResponseEntity.ok(GenericResponse.exito("Estatus de la cuenta actualizado correctamente", cuenta));
    }
}
