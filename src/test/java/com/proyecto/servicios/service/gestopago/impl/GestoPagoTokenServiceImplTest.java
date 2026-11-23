package com.proyecto.servicios.service.gestopago.impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.model.GestoPagoAuthResponse;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.gestopago.ProveedorNoDisponibleException;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.repository.gestopago.GestoPagoTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: GestoPagoTokenServiceImpl")
class GestoPagoTokenServiceImplTest {

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private GestoPagoTokenRepository tokenRepository;

    @Mock
    private GestoPagoTokenMapper tokenMapper;

    private GestoPagoTokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new GestoPagoTokenServiceImpl(gestoPagoAuthClient, tokenRepository, tokenMapper);
        ReflectionTestUtils.setField(tokenService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(tokenService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(tokenService, "password", "secret");
    }

    @Test
    @DisplayName("obtenerTokenBearerValido: retorna token existente en BD")
    void obtenerToken_existente() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token-existente-123");

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(token));

        String resultado = tokenService.obtenerTokenBearerValido();

        assertEquals("token-existente-123", resultado);
        verify(gestoPagoAuthClient, never()).authenticate(any(), any(), any());
    }

    @Test
    @DisplayName("obtenerTokenBearerValido: si no existe en BD, fuerza renovación con cliente Feign")
    void obtenerToken_noExiste_fuerzaRenovacion() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.empty());

        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setToken("nuevo-token-feign");

        when(gestoPagoAuthClient.authenticate(83, "GPS83-TPV-17", "secret")).thenReturn(authResp);
        when(tokenMapper.toEntity(authResp)).thenReturn(new GestoPagoToken());
        when(tokenRepository.save(any(GestoPagoToken.class))).thenAnswer(inv -> {
            GestoPagoToken t = inv.getArgument(0);
            t.setToken("nuevo-token-feign");
            return t;
        });

        String resultado = tokenService.obtenerTokenBearerValido();

        assertEquals("nuevo-token-feign", resultado);
        verify(gestoPagoAuthClient).authenticate(83, "GPS83-TPV-17", "secret");
    }

    @Test
    @DisplayName("forzarRenovacionYObtenerToken: actualiza token existente en BD")
    void forzarRenovacion_actualizaExistente() {
        GestoPagoToken existente = new GestoPagoToken();
        existente.setToken("viejo-token");

        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setToken("token-renovado-456");

        when(gestoPagoAuthClient.authenticate(83, "GPS83-TPV-17", "secret")).thenReturn(authResp);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(existente));
        when(tokenRepository.save(existente)).thenAnswer(inv -> {
            existente.setToken("token-renovado-456");
            return existente;
        });

        String resultado = tokenService.forzarRenovacionYObtenerToken();

        assertEquals("token-renovado-456", resultado);
        verify(tokenMapper).updateEntity(authResp, existente);
    }

    @Test
    @DisplayName("renovarToken: maneja excepciones sin propagar error")
    void renovarToken_manejaExcepcion() {
        when(gestoPagoAuthClient.authenticate(any(), any(), any())).thenThrow(new RuntimeException("Error red"));

        assertDoesNotThrow(() -> tokenService.renovarToken());
    }

    @Test
    @DisplayName("ejecutarAutenticacion: falla si proveedor retorna null o token vacío")
    void autenticar_respuestaInvalida() {
        when(gestoPagoAuthClient.authenticate(any(), any(), any())).thenReturn(null);

        assertThrows(ProveedorNoDisponibleException.class, () -> tokenService.forzarRenovacionYObtenerToken());
    }
}

