package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.config.security.JwtService;
import com.proyecto.servicios.dto.request.auth.LoginRequest;
import com.proyecto.servicios.dto.request.auth.VerificarOtpRequest;
import com.proyecto.servicios.dto.response.auth.LoginResponse;
import com.proyecto.servicios.entity.banco.SesionActiva;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.banco.CredencialesInvalidasException;
import com.proyecto.servicios.exception.banco.PinInvalidoException;
import com.proyecto.servicios.exception.banco.UsuarioInactivoException;
import com.proyecto.servicios.exception.banco.UsuarioNoEncontradoException;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.service.banco.AuthService;
import com.proyecto.servicios.service.banco.SesionService;
import com.proyecto.servicios.util.TotpUtil;
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
    private static final String METODO_GOOGLE_AUTHENTICATOR = "GOOGLE_AUTHENTICATOR";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
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

        // 5. Segundo Factor exclusivo con Google Authenticator (TOTP)
        if (tieneTotpActivo(usuario)) {
            log.info("Paso 1 superado. Solicitando código de Google Authenticator para: {}", correo);
            return LoginResponse.builder()
                    .correo(usuario.getCorreo())
                    .requiere2fa(true)
                    .metodo2fa(METODO_GOOGLE_AUTHENTICATOR)
                    .requiereConfiguracionTotp(false)
                    .mensaje("Ingresa el código de 6 dígitos de tu app Google Authenticator.")
                    .build();
        }

        String secreto = usuario.getTotpSecret();
        if (secreto == null || secreto.isBlank()) {
            secreto = TotpUtil.generarSecretoBase32();
            usuario.setTotpSecret(secreto);
            usuario.setTotpHabilitado(false);
            usuarioRepository.save(usuario);
        }

        String otpAuthUri = TotpUtil.construirOtpAuthUri(usuario.getCorreo(), secreto);
        String qrBase64 = TotpUtil.generarQrPngBase64(otpAuthUri);

        log.info("Paso 1 superado. Solicitando vinculación inicial de Google Authenticator para: {}", correo);
        return LoginResponse.builder()
                .correo(usuario.getCorreo())
                .requiere2fa(true)
                .metodo2fa(METODO_GOOGLE_AUTHENTICATOR)
                .requiereConfiguracionTotp(true)
                .totpSecreto(secreto)
                .totpOtpAuthUri(otpAuthUri)
                .totpQrBase64(qrBase64)
                .mensaje("Vincula tu cuenta en Google Authenticator con el código QR o la clave manual e ingresa el código de 6 dígitos.")
                .build();
    }

    @Override
    @Transactional
    public LoginResponse verificarOtp(VerificarOtpRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();
        log.info("Verificando código Google Authenticator para usuario: {}", correo);

        Usuario usuario = obtenerUsuarioActivo(correo);

        if (usuario.getTotpSecret() == null || usuario.getTotpSecret().isBlank()) {
            throw new ReglaNegocioException("Debes iniciar sesión nuevamente para configurar Google Authenticator.");
        }

        if (!TotpUtil.verificarCodigo(usuario.getTotpSecret(), request.getCodigo())) {
            throw new PinInvalidoException("Código de Google Authenticator incorrecto. Verifica e intenta de nuevo.");
        }

        if (!Boolean.TRUE.equals(usuario.getTotpHabilitado())) {
            usuario.setTotpHabilitado(true);
            usuarioRepository.save(usuario);
        }

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

    private boolean tieneTotpActivo(Usuario usuario) {
        return Boolean.TRUE.equals(usuario.getTotpHabilitado())
                && usuario.getTotpSecret() != null
                && !usuario.getTotpSecret().isBlank();
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
