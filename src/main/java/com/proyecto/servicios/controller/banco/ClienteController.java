package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.dto.request.banco.*;
import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.ClienteResponse;
import com.proyecto.servicios.dto.response.banco.DisponibilidadRegistroResponse;
import com.proyecto.servicios.service.banco.ClienteService;
import com.proyecto.servicios.service.banco.VerificacionCorreoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Operaciones de Onboarding, consulta dinámica y actualización de clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final VerificacionCorreoService verificacionCorreoService;

    @PostMapping
    @Operation(summary = "Registrar un nuevo cliente con cuenta bancaria, usuario de acceso y biometría opcional")
    public ResponseEntity<GenericResponse<ClienteResponse>> registrar(
            @Valid @RequestBody RegistroClienteRequest request) {
        ClienteResponse respuesta = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito("Cliente registrado exitosamente", respuesta));
    }

    @GetMapping("/disponibilidad")
    @Operation(summary = "Verificar disponibilidad de CURP, RFC, correo electrónico y teléfono móvil antes de avanzar en el registro")
    public ResponseEntity<GenericResponse<DisponibilidadRegistroResponse>> verificarDisponibilidad(
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String telefonoMovil) {
        DisponibilidadRegistroResponse respuesta = clienteService.verificarDisponibilidad(curp, rfc, correo, telefonoMovil);
        return ResponseEntity.ok(GenericResponse.exito("Disponibilidad verificada correctamente", respuesta));
    }

    @PostMapping("/verificar-correo/enviar")
    @Operation(summary = "Enviar código de 6 dígitos para comprobar existencia del correo antes de crear la cuenta")
    public ResponseEntity<GenericResponse<Map<String, Object>>> enviarCodigoCorreo(
            @RequestBody Map<String, String> payload) {
        int segundos = verificacionCorreoService.enviarCodigoVerificacion(payload.get("correo"));
        return ResponseEntity.ok(GenericResponse.exito(
                "Código de verificación enviado a tu correo",
                Map.of("segundosExpiracion", segundos)
        ));
    }

    @PostMapping("/verificar-correo/confirmar")
    @Operation(summary = "Confirmar código de verificación del correo previo al registro")
    public ResponseEntity<GenericResponse<Boolean>> confirmarCodigoCorreo(
            @RequestBody Map<String, String> payload) {
        boolean confirmado = verificacionCorreoService.confirmarCodigoVerificacion(
                payload.get("correo"), payload.get("codigo"));
        return ResponseEntity.ok(GenericResponse.exito("Correo verificado correctamente", confirmado));
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
