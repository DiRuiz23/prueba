package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.DatosBiometricos;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.repository.banco.DatosBiometricosRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: BiometriaServiceImpl")
class BiometriaServiceImplTest {

    @Mock
    private DatosBiometricosRepository datosBiometricosRepository;

    @InjectMocks
    private BiometriaServiceImpl biometriaService;

    @Test
    @DisplayName("procesarYGuardarBiometria: null o vacío retorna null sin guardar")
    void procesar_nullOVacio_retornaNull() {
        Cliente c = Cliente.builder().id(1L).build();
        assertNull(biometriaService.procesarYGuardarBiometria(c, null));
        assertNull(biometriaService.procesarYGuardarBiometria(c, "   "));
        verify(datosBiometricosRepository, never()).save(any());
    }

    @Test
    @DisplayName("procesarYGuardarBiometria: base64 menor a 5KB lanza ReglaNegocioException")
    void procesar_calidadInsuficiente() {
        Cliente c = Cliente.builder().id(1L).build();
        byte[] bytesPequenos = new byte[100];
        String b64 = Base64.getEncoder().encodeToString(bytesPequenos);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> biometriaService.procesarYGuardarBiometria(c, b64));
        assertTrue(ex.getMessage().contains("insuficiente"));
    }

    @Test
    @DisplayName("procesarYGuardarBiometria: base64 no válido lanza ReglaNegocioException")
    void procesar_base64Invalido() {
        Cliente c = Cliente.builder().id(1L).build();
        assertThrows(ReglaNegocioException.class,
                () -> biometriaService.procesarYGuardarBiometria(c, "no-es-base64-valido!!!@@@"));
    }

    @Test
    @DisplayName("procesarYGuardarBiometria: flujo exitoso con data URI")
    void procesar_exitoso() {
        Cliente c = Cliente.builder().id(1L).build();
        byte[] bytesValidos = new byte[6000];
        String b64 = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytesValidos);

        when(datosBiometricosRepository.save(any(DatosBiometricos.class))).thenAnswer(inv -> inv.getArgument(0));

        DatosBiometricos resultado = biometriaService.procesarYGuardarBiometria(c, b64);

        assertNotNull(resultado);
        assertNotNull(resultado.getFacialHash());
        assertEquals("INTERNAL_BIOMETRIC_ENGINE", resultado.getProveedorReconocimiento());
        assertTrue(resultado.getActivo());
        verify(datosBiometricosRepository).save(any(DatosBiometricos.class));
    }
}

