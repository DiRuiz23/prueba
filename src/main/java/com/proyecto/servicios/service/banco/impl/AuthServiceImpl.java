package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.config.security.JwtService;
import com.proyecto.servicios.dto.request.auth.LoginRequest;
import com.proyecto.servicios.dto.request.auth.VerificarOtpRequest;
import com.proyecto.servicios.dto.response.auth.LoginResponse;
import com.proyecto.servicios.entity.banco.SesionActiva;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.CredencialesInvalidasException;
import com.proyecto.servicios.exception.banco.UsuarioInactivoException;
import com.proyecto.servicios.exception.banco.UsuarioNoEncontradoException;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.service.banco.AuthService;
import com.proyecto.servicios.service.banco.OtpService;
import com.proyecto.servicios.service.banco.SesionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String USUARIO_NO_ENCONTRADO_MSG = "No existe un usuario registrado con el correo: ";
    private static final String USUARIO_INACTIVO_MSG = "El usuario está inactivo. Comuníquese con soporte bancario.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final SesionService sesionService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();
        log.info("Intento de autenticación para: {}", correo);

        Usuario usuario = obtenerUsuarioActivo(correo);

        // 3. Verificar credenciales primarias (contraseña)
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("Credenciales inválidas para el correo: {}", correo);
            throw new CredencialesInvalidasException("Las credenciales proporcionadas son incorrectas.");
        }

        // 4. Verificación de exclusividad de sesión en servidor
        Optional<SesionActiva> sesionPrevia = sesionService.obtenerSesionActivaPrevia(usuario.getId());
        if (sesionPrevia.isPresent() && !Boolean.TRUE.equals(request.getForzarCierreSesionAnterior())) {
            String dispPrevio = sesionPrevia.get().getDispositivo();
            log.info("Sesión activa detectada en dispositivo previo ({}) para {}", dispPrevio, correo);
            return LoginResponse.builder()
                    .correo(usuario.getCorreo())
                    .sesionActivaDetectada(true)
                    .dispositivoSesionActiva(dispPrevio)
                    .mensaje("Se detectó una sesión activa en otro dispositivo (" + dispPrevio +
                            "). ¿Desea finalizarla para acceder?")
                    .build();
        }

        // Si se indicó forzar cierre, se invalidan de inmediato las sesiones anteriores
        if (Boolean.TRUE.equals(request.getForzarCierreSesionAnterior())) {
            sesionService.invalidarSesionesPrevias(usuario.getId());
        }

        // 5. Iniciar Segundo Factor de Autenticación (2FA - OTP)
        int tiempoExpiracion = otpService.generarYEnviarOtp(usuario, OtpServiceImpl.PROPOSITO_LOGIN_2FA);

        log.info("Paso 1 de autenticación superado. Requiere 2FA para: {}", correo);
        return LoginResponse.builder()
                .correo(usuario.getCorreo())
                .requiere2fa(true)
                .tiempoExpiracionOtpSegundos(tiempoExpiracion)
                .mensaje("Código de seguridad 2FA enviado. Ingréselo para completar su acceso.")
                .build();
    }

    @Override
    @Transactional
    public LoginResponse verificarOtp(VerificarOtpRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();
        log.info("Verificando código 2FA para usuario: {}", correo);

        Usuario usuario = obtenerUsuarioActivo(correo);

        // 1. Validar OTP en servidor
        otpService.validarOtp(usuario, request.getCodigo(), OtpServiceImpl.PROPOSITO_LOGIN_2FA);

        // 2. Generar identificador de sesión único (JTI)
        String jti = UUID.randomUUID().toString();

        // 3. Registrar sesión activa única respaldada en base de datos
        sesionService.registrarNuevaSesion(usuario, jti, request.getDispositivo(), request.getIpOrigen());

        // 4. Generar Token JWT firmado vinculado al JTI
        User userDetails = new User(usuario.getCorreo(), usuario.getPassword(), Collections.emptyList());
        String token = jwtService.generarToken(userDetails, jti);

        log.info("Autenticación multifactor completada exitosamente para: {}", correo);
        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .correo(usuario.getCorreo())
                .requiere2fa(false)
                .mensaje("Acceso autorizado con éxito.")
                .build();
    }

    private Usuario obtenerUsuarioActivo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(USUARIO_NO_ENCONTRADO_MSG + correo));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("Intento de acceso de usuario inactivo: {}", correo);
            throw new UsuarioInactivoException(USUARIO_INACTIVO_MSG);
        }
        return usuario;
    }

    @Override
    @Transactional
    public boolean logout(String tokenBearer) {
        if (tokenBearer == null || tokenBearer.isBlank()) {
            return false;
        }
        String token = tokenBearer.startsWith("Bearer ") ? tokenBearer.substring(7) : tokenBearer;
        String jti = jwtService.extraerJti(token);
        log.info("Cerrando sesión para token con JTI: {}", jti);
        return sesionService.cerrarSesion(jti);
    }

    @Override
    @Transactional
    public boolean forzarCierreSesion(String correo) {
        String correoLimpio = correo != null ? correo.trim().toLowerCase() : "";
        Optional<Usuario> optUser = usuarioRepository.findByCorreo(correoLimpio);
        if (optUser.isPresent()) {
            sesionService.invalidarSesionesPrevias(optUser.get().getId());
            return true;
        }
        return false;
    }
}
