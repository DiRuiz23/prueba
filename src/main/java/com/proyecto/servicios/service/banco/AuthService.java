package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.dto.request.auth.LoginRequest;
import com.proyecto.servicios.dto.request.auth.VerificarOtpRequest;
import com.proyecto.servicios.dto.response.auth.LoginResponse;

public interface AuthService {

    /**
     * Fase 1 de Autenticación: Valida usuario y contraseña.
     * Si las credenciales son válidas y 2FA está activo, genera un OTP y solicita validación 2FA.
     * Si se detecta sesión activa en otro dispositivo y no se solicitó forzar cierre, alerta al usuario.
     */
    LoginResponse login(LoginRequest request);

    /**
     * Fase 2 de Autenticación: Valida código OTP de 6 dígitos.
     * En caso de éxito, invalida sesiones previas, registra la nueva sesión única y emite el JWT final.
     */
    LoginResponse verificarOtp(VerificarOtpRequest request);

    /**
     * Invalida la sesión actual del usuario en la base de datos (Logout seguro).
     */
    boolean logout(String tokenBearer);

    /**
     * Cierra forzosamente las sesiones activas previas de un usuario.
     */
    boolean forzarCierreSesion(String correo);
}
