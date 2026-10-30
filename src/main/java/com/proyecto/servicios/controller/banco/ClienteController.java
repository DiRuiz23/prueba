package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.dto.request.banco.*;
import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.ClienteResponse;
import com.proyecto.servicios.service.banco.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Operaciones de Onboarding, consulta dinámica y actualización de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @Operation(summary = "Registrar un nuevo cliente con cuenta bancaria, usuario de acceso y biometría opcional")
    public ResponseEntity<GenericResponse<ClienteResponse>> registrar(
            @Valid @RequestBody RegistroClienteRequest request) {
        ClienteResponse respuesta = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito("Cliente registrado exitosamente", respuesta));
    }

    @PostMapping("/consultar")
    @Operation(summary = "Consultar clientes con filtros dinámicos combinables y paginación")
    public ResponseEntity<GenericResponse<PaginaResponse<ClienteResponse>>> consultar(
            @Valid @RequestBody ClienteFiltroRequest request) {
        PaginaResponse<ClienteResponse> pagina = clienteService.consultar(request);
        return ResponseEntity.ok(GenericResponse.exito("Consulta de clientes realizada correctamente", pagina));
    }

    @PostMapping("/detalle")
    @Operation(summary = "Consultar un cliente por su ID numérico (enviado en el cuerpo)")
    public ResponseEntity<GenericResponse<ClienteResponse>> consultarPorId(
            @Valid @RequestBody ClienteIdRequest request) {
        ClienteResponse cliente = clienteService.consultarPorId(request);
        return ResponseEntity.ok(GenericResponse.exito("Cliente encontrado", cliente));
    }

    @PostMapping("/cuenta")
    @Operation(summary = "Consultar el cliente asociado a un número de cuenta (enviado en el cuerpo)")
    public ResponseEntity<GenericResponse<ClienteResponse>> consultarPorNumeroCuenta(
            @Valid @RequestBody NumeroCuentaRequest request) {
        ClienteResponse cliente = clienteService.consultarPorNumeroCuenta(request);
        return ResponseEntity.ok(GenericResponse.exito("Cliente encontrado", cliente));
    }

    @PatchMapping
    @Operation(summary = "Actualizar información del cliente (ID en el cuerpo, CURP y RFC inmutables)")
    public ResponseEntity<GenericResponse<ClienteResponse>> actualizar(
            @Valid @RequestBody ActualizarClienteRequest request) {
        ClienteResponse actualizado = clienteService.actualizarCliente(request);
        return ResponseEntity.ok(GenericResponse.exito("Cliente actualizado correctamente", actualizado));
    }

    @PostMapping("/baja")
    @Operation(summary = "Baja lógica del cliente (desactiva usuario y cuentas asociadas)")
    public ResponseEntity<GenericResponse<Void>> bajaLogica(
            @Valid @RequestBody ClienteIdRequest request) {
        clienteService.bajaLogicaCliente(request);
        return ResponseEntity.ok(GenericResponse.exitoSinDatos("Cliente dado de baja correctamente"));
    }
}
