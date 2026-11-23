package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.SesionActiva;
import com.proyecto.servicios.entity.banco.Usuario;

import java.util.Optional;

public interface SesionService {

    /**
     * Registra una nueva sesión activa para el usuario, invalidando las anteriores si se indica.
     */
    SesionActiva registrarNuevaSesion(Usuario usuario, String jti, String dispositivo, String ipOrigen);

    /**
     * Verifica si una sesión con el JTI dado está activa en el servidor.
     */
    boolean esSesionActiva(String jti);

    /**
     * Invalida la sesión actual identificada por su JTI (Logout seguro).
     */
    boolean cerrarSesion(String jti);

    /**
     * Invalida todas las sesiones previas activas de un usuario.
     */
    int invalidarSesionesPrevias(Long usuarioId);

    /**
     * Obtiene la sesión activa previa si existe (para informar al usuario en login concurrente).
     */
    Optional<SesionActiva> obtenerSesionActivaPrevia(Long usuarioId);
}

