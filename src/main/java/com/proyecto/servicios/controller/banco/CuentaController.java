package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.model.banco.ActualizarEstatusCuentaRequest;
import com.proyecto.servicios.model.banco.CrearCuentaRequest;
import com.proyecto.servicios.model.banco.CuentaResponse;
import com.proyecto.servicios.service.banco.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas Bancarias", description = "Gestión de cuentas, consulta de saldo, bloqueo y desbloqueo")
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping("/{numeroCuenta}")
    @Operation(summary = "Consultar cuenta por su número único")
    public ResponseEntity<CuentaResponse> consultarPorNumero(
            @PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping("/{numeroCuenta}/saldo")
    @Operation(summary = "Consultar saldo disponible de una cuenta")
    public ResponseEntity<BigDecimal> consultarSaldo(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarSaldo(numeroCuenta));
    }

    @GetMapping
    @Operation(summary = "Consultar cuentas por cliente ID, por estatus (ACTIVA, BLOQUEADA, INACTIVA) o todas")
    public ResponseEntity<List<CuentaResponse>> buscarCuentas(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Cuenta.EstatusCuenta estatus) {
        if (clienteId != null) {
            return ResponseEntity.ok(cuentaService.consultarPorClienteId(clienteId));
        }
        if (estatus != null) {
            return ResponseEntity.ok(cuentaService.consultarPorEstatus(estatus));
        }
        return ResponseEntity.ok(cuentaService.consultarTodas());
    }

    @PostMapping
    @Operation(summary = "Crear una cuenta bancaria adicional para un cliente activo existente")
    public ResponseEntity<CuentaResponse> crearCuenta(
            @Valid @RequestBody CrearCuentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuentaService.crearCuentaAdicional(request));
    }

    @PatchMapping("/{numeroCuenta}")
    @Operation(summary = "Actualizar el estatus de una cuenta bancaria (ACTIVA, BLOQUEADA, INACTIVA)")
    public ResponseEntity<CuentaResponse> cambiarEstatus(
            @PathVariable String numeroCuenta,
            @Valid @RequestBody ActualizarEstatusCuentaRequest request) {
        return ResponseEntity.ok(
                cuentaService.cambiarEstatusCuenta(numeroCuenta, request.getNuevoEstatus()));
    }
}
