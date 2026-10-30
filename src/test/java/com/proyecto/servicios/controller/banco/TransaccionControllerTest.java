package com.proyecto.servicios.controller.banco;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.security.JwtAuthenticationFilter;
import com.proyecto.servicios.dto.request.banco.PagoServicioRequest;
import com.proyecto.servicios.dto.response.banco.PagoServicioResponse;
import com.proyecto.servicios.exception.banco.IdempotenciaConflictoException;
import com.proyecto.servicios.exception.banco.PinBloqueadoException;
import com.proyecto.servicios.exception.banco.SaldoInsuficienteException;
import com.proyecto.servicios.exception.gestopago.ProveedorException;
import com.proyecto.servicios.exception.gestopago.ResultadoInciertoException;
import com.proyecto.servicios.service.banco.PagoServicioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransaccionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TransaccionController (Pruebas HTTP y Manejo de Respuestas)")
class TransaccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PagoServicioService pagoServicioService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final Principal principal = new UsernamePasswordAuthenticationToken("cliente@banco.com", "pass");

    private PagoServicioRequest requestValido() {
        return PagoServicioRequest.builder()
                .numeroCuenta("01200000000000000016")
                .idProducto(582)
                .idServicio(133)
                .telefonoReferencia("5512345678")
                .monto(new BigDecimal("50.00"))
                .pin("1234")
                .build();
    }

    @Test
    @DisplayName("POST /pago-servicio con éxito → HTTP 200 con comprobante")
    void pagarServicio_exito_retorna200() throws Exception {
        PagoServicioResponse response = PagoServicioResponse.builder()
                .idTransaccion(1L)
                .upc("GP000000000001")
                .idTxGestoPago("TX-12345")
                .numeroAutorizacion("AUTH-67890")
                .numeroCuenta("01200000000000000016")
                .montoDebitado(new BigDecimal("50.00"))
                .saldoRestante(new BigDecimal("950.00"))
                .estatus("EXITOSA")
                .mensaje("Operación realizada con éxito")
                .fechaTransaccion(OffsetDateTime.now())
                .build();

        when(pagoServicioService.realizarPagoServicio(any(), eq("cliente@banco.com"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/transacciones/pago-servicio")
                        .principal(principal)
                        .header("Idempotency-Key", "uuid-1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(0))
                .andExpect(jsonPath("$.datos.upc").value("GP000000000001"))
                .andExpect(jsonPath("$.datos.estatus").value("EXITOSA"));
    }

    @Test
    @DisplayName("POST /pago-servicio sin cabecera Idempotency-Key → HTTP 400 BAD REQUEST")
    void pagarServicio_sinIdempotencyKey_retorna400() throws Exception {
        mockMvc.perform(post("/transacciones/pago-servicio")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La cabecera HTTP obligatoria 'Idempotency-Key' no fue proporcionada"));
    }

    @Test
    @DisplayName("POST /pago-servicio con resultado incierto (timeout/código 82) → HTTP 202 ACCEPTED")
    void pagarServicio_resultadoIncierto_retorna202() throws Exception {
        when(pagoServicioService.realizarPagoServicio(any(), eq("cliente@banco.com"), any()))
                .thenThrow(new ResultadoInciertoException("Tiempo de espera agotado al comunicar con GestoPago.", null));

        mockMvc.perform(post("/transacciones/pago-servicio")
                        .principal(principal)
                        .header("Idempotency-Key", "uuid-202")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje").value("Tiempo de espera agotado al comunicar con GestoPago."));
    }

    @Test
    @DisplayName("POST /pago-servicio con PIN bloqueado → HTTP 423 LOCKED")
    void pagarServicio_pinBloqueado_retorna423() throws Exception {
        when(pagoServicioService.realizarPagoServicio(any(), eq("cliente@banco.com"), any()))
                .thenThrow(new PinBloqueadoException("PIN bloqueado por 15 minutos por exceso de intentos."));

        mockMvc.perform(post("/transacciones/pago-servicio")
                        .principal(principal)
                        .header("Idempotency-Key", "uuid-423")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.mensaje").value("PIN bloqueado por 15 minutos por exceso de intentos."));
    }

    @Test
    @DisplayName("POST /pago-servicio con conflicto de idempotencia → HTTP 409 CONFLICT")
    void pagarServicio_idempotenciaConflicto_retorna409() throws Exception {
        when(pagoServicioService.realizarPagoServicio(any(), eq("cliente@banco.com"), any()))
                .thenThrow(new IdempotenciaConflictoException("Clave de idempotencia duplicada."));

        mockMvc.perform(post("/transacciones/pago-servicio")
                        .principal(principal)
                        .header("Idempotency-Key", "uuid-409")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /pago-servicio con saldo insuficiente → HTTP 422 UNPROCESSABLE ENTITY")
    void pagarServicio_saldoInsuficiente_retorna422() throws Exception {
        when(pagoServicioService.realizarPagoServicio(any(), eq("cliente@banco.com"), any()))
                .thenThrow(new SaldoInsuficienteException("Fondos insuficientes en la cuenta."));

        mockMvc.perform(post("/transacciones/pago-servicio")
                        .principal(principal)
                        .header("Idempotency-Key", "uuid-422")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /pago-servicio con rechazo del proveedor → HTTP 502 BAD GATEWAY")
    void pagarServicio_proveedorRechazo_retorna502() throws Exception {
        when(pagoServicioService.realizarPagoServicio(any(), eq("cliente@banco.com"), any()))
                .thenThrow(new ProveedorException("Saldo insuficiente del distribuidor", "02"));

        mockMvc.perform(post("/transacciones/pago-servicio")
                        .principal(principal)
                        .header("Idempotency-Key", "uuid-502")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("GET /transacciones/{id} → HTTP 200 con estado de la transacción")
    void consultarTransaccion_retorna200() throws Exception {
        PagoServicioResponse response = PagoServicioResponse.builder()
                .idTransaccion(1L)
                .upc("GP000000000001")
                .estatus("EXITOSA")
                .build();

        when(pagoServicioService.consultarEstadoTransaccion(1L, "cliente@banco.com"))
                .thenReturn(response);

        mockMvc.perform(get("/transacciones/1")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.idTransaccion").value(1))
                .andExpect(jsonPath("$.datos.estatus").value("EXITOSA"));
    }
}
