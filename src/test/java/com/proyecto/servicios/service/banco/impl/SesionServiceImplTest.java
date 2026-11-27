package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.banco.SesionActiva;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.repository.banco.SesionActivaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: SesionServiceImpl (Sesión única y exclusividad)")
class SesionServiceImplTest {

    @Mock
    private SesionActivaRepository sesionActivaRepository;

    @InjectMocks
    private SesionServiceImpl sesionService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sesionService, "jwtExpirationMs", 3600000L);
        usuario = Usuario.builder().id(10L).correo("usuario@test.com").activo(true).build();
    }

    @Test
    @DisplayName("registrarNuevaSesion: invalida sesiones previas y persiste nueva sesión")
    void registrarNuevaSesion_exitoso() {
        when(sesionActivaRepository.save(any(SesionActiva.class))).thenAnswer(inv -> inv.getArgument(0));

        SesionActiva resultado = sesionService.registrarNuevaSesion(usuario, "jti-123", "MOVIL", "192.168.1.1");

        assertNotNull(resultado);
        assertEquals("jti-123", resultado.getJti());
        assertEquals("MOVIL", resultado.getDispositivo());
        assertEquals("192.168.1.1", resultado.getIpOrigen());
        assertTrue(resultado.getActiva());
        verify(sesionActivaRepository).invalidarSesionesActivasPorUsuario(10L);
        verify(sesionActivaRepository).save(any(SesionActiva.class));
    }

    @Test
    @DisplayName("esSesionActiva: retorna true cuando sesión existe, está activa y no ha expirado")
    void esSesionActiva_vigente() {
        SesionActiva sesion = SesionActiva.builder()
                .jti("jti-valido")
                .activa(true)
                .fechaExpiracion(OffsetDateTime.now(ZoneId.systemDefault()).plusHours(1))
                .build();

        when(sesionActivaRepository.findByJtiAndActivaTrue("jti-valido")).thenReturn(Optional.of(sesion));

        assertTrue(sesionService.esSesionActiva("jti-valido"));
    }

    @Test
    @DisplayName("esSesionActiva: retorna false cuando sesión ya expiró")
    void esSesionActiva_expirada() {
        SesionActiva sesion = SesionActiva.builder()
                .jti("jti-expirado")
                .activa(true)
                .fechaExpiracion(OffsetDateTime.now(ZoneId.systemDefault()).minusMinutes(5))
                .build();

        when(sesionActivaRepository.findByJtiAndActivaTrue("jti-expirado")).thenReturn(Optional.of(sesion));

        assertFalse(sesionService.esSesionActiva("jti-expirado"));
    }

    @Test
    @DisplayName("esSesionActiva: retorna false si JTI no existe o es nulo/vacío")
    void esSesionActiva_noExiste() {
        assertFalse(sesionService.esSesionActiva(null));
        assertFalse(sesionService.esSesionActiva(""));
        when(sesionActivaRepository.findByJtiAndActivaTrue("jti-inexistente")).thenReturn(Optional.empty());
        assertFalse(sesionService.esSesionActiva("jti-inexistente"));
    }

    @Test
    @DisplayName("cerrarSesion: invalida por JTI correctamente")
    void cerrarSesion_exitoso() {
        when(sesionActivaRepository.invalidarSesionPorJti("jti-logout")).thenReturn(1);
        assertTrue(sesionService.cerrarSesion("jti-logout"));

        assertFalse(sesionService.cerrarSesion(null));
        assertFalse(sesionService.cerrarSesion(" "));
    }

    @Test
    @DisplayName("obtenerSesionActivaPrevia: consulta sesión previa del usuario")
    void obtenerSesionActivaPrevia_exitoso() {
        SesionActiva sesion = SesionActiva.builder().id(1L).dispositivo("WEB").build();
        when(sesionActivaRepository.findFirstByUsuarioIdAndActivaTrueOrderByFechaCreacionDesc(10L))
                .thenReturn(Optional.of(sesion));

        Optional<SesionActiva> opt = sesionService.obtenerSesionActivaPrevia(10L);
        assertTrue(opt.isPresent());
        assertEquals("WEB", opt.get().getDispositivo());
    }
}

