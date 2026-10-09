package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.banco.ActualizarClientePatchRequest;
import com.proyecto.servicios.model.banco.ClienteResponse;
import com.proyecto.servicios.model.banco.RegistroClienteRequest;
import com.proyecto.servicios.service.banco.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Operaciones de Onboarding, consulta y actualización de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @Operation(summary = "Registrar un nuevo cliente con cuenta bancaria, usuario de acceso y biometría")
    public ResponseEntity<ClienteResponse> registrar(
            @Valid @RequestBody RegistroClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.registrarCliente(request));
    }

    @GetMapping
    @Operation(summary = "Consultar clientes con filtros dinámicos (nombre, apellidos, CURP, RFC, numeroCuenta, rango de fechas o solo activos)")
    public ResponseEntity<List<ClienteResponse>> consultarClientes(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellidoPaterno,
            @RequestParam(required = false) String apellidoMaterno,
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc,
            @RequestParam(required = false) String numeroCuenta,
            @RequestParam(required = false) Boolean soloActivos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaFin) {

        // Filtro por número de cuenta (consulta específica de requerimiento)
        if (numeroCuenta != null && !numeroCuenta.isBlank()) {
            return ResponseEntity.ok(List.of(clienteService.consultarPorNumeroCuenta(numeroCuenta)));
        }

        return ResponseEntity.ok(clienteService.buscarConFiltros(
                nombre, apellidoPaterno, apellidoMaterno,
                curp, rfc, soloActivos, fechaInicio, fechaFin));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un cliente por su ID numérico")
    public ResponseEntity<ClienteResponse> consultarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.consultarPorId(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar parcialmente un cliente (CURP y RFC son inmutables)")
    public ResponseEntity<ClienteResponse> actualizarParcial(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarClientePatchRequest request) {
        return ResponseEntity.ok(clienteService.actualizarClienteParcial(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Baja lógica del cliente (desactiva usuario y cuentas asociadas)")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        clienteService.bajaLogicaCliente(id);
        return ResponseEntity.noContent().build();
    }
}
