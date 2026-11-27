package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.banco.SesionActiva;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.repository.banco.SesionActivaRepository;
import com.proyecto.servicios.service.banco.SesionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SesionServiceImpl implements SesionService {

    private final SesionActivaRepository sesionActivaRepository;

    @Value("${banco.security.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Override
    @Transactional
    public SesionActiva registrarNuevaSesion(Usuario usuario, String jti, String dispositivo, String ipOrigen) {
        log.info("Registrando nueva sesión para usuario {} en dispositivo {}", usuario.getCorreo(), dispositivo);
        // Exclusividad de sesión: invalidar sesiones previas activas del usuario
        log.info("Invalidando sesiones previas para usuarioId: {}", usuario.getId());
        sesionActivaRepository.invalidarSesionesActivasPorUsuario(usuario.getId());

        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.systemDefault());
        OffsetDateTime expiracion = ahora.plusSeconds(jwtExpirationMs / 1000);

        String disp = (dispositivo != null && !dispositivo.isBlank())
                ? dispositivo.trim().toUpperCase()
                : "DESCONOCIDO";

        SesionActiva sesion = SesionActiva.builder()
                .usuario(usuario)
                .jti(jti)
                .dispositivo(disp)
                .ipOrigen(ipOrigen)
                .activa(true)
                .fechaCreacion(ahora)
                .fechaUltimoAcceso(ahora)
                .fechaExpiracion(expiracion)
                .build();

        return sesionActivaRepository.save(sesion);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esSesionActiva(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Optional<SesionActiva> sesion = sesionActivaRepository.findByJtiAndActivaTrue(jti);
        if (sesion.isEmpty()) {
            return false;
        }
        SesionActiva s = sesion.get();
        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.systemDefault());
        return s.getFechaExpiracion() != null && s.getFechaExpiracion().isAfter(ahora);
    }

    @Override
    @Transactional
    public boolean cerrarSesion(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        log.info("Cerrando sesión para JTI: {}", jti);
        int actualizados = sesionActivaRepository.invalidarSesionPorJti(jti);
        return actualizados > 0;
    }

    @Override
    @Transactional
    public int invalidarSesionesPrevias(Long usuarioId) {
        log.info("Invalidando sesiones previas para usuarioId: {}", usuarioId);
        return sesionActivaRepository.invalidarSesionesActivasPorUsuario(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SesionActiva> obtenerSesionActivaPrevia(Long usuarioId) {
        return sesionActivaRepository.findFirstByUsuarioIdAndActivaTrueOrderByFechaCreacionDesc(usuarioId);
    }
}

