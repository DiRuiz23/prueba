package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.NoAutenticadoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.banco.CuentaBloqueadaException;
import com.proyecto.servicios.exception.banco.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.banco.PinBloqueadoException;
import com.proyecto.servicios.exception.banco.PinInvalidoException;
import com.proyecto.servicios.exception.banco.UsuarioInactivoException;
import com.proyecto.servicios.repository.banco.CuentaRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.service.banco.AutorizacionOperacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutorizacionOperacionServiceImpl implements AutorizacionOperacionService {

    /** Mensaje único para "no existe" y "no es tuya": evita enumerar cuentas ajenas. */
    static final String CUENTA_NO_ENCONTRADA = "No se encontró la cuenta indicada";
    public static final int MAX_INTENTOS_PIN = 3;
    public static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Cuenta obtenerCuentaAutorizada(String numeroCuenta, String correoAutenticado) {
        Usuario usuario = usuarioRepository.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new NoAutenticadoException("No fue posible verificar la identidad del solicitante"));

        if (!Boolean.TRUE.equals(usuario.getActivo()) || !Boolean.TRUE.equals(usuario.getCliente().getActivo())) {
            throw new UsuarioInactivoException("El usuario está inactivo. Comuníquese con soporte bancario.");
        }

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .filter(c -> Objects.equals(c.getCliente().getId(), usuario.getCliente().getId()))
                .orElseThrow(() -> {
                    log.warn("Acceso a cuenta no encontrada o ajena. usuarioId={}", usuario.getId());
                    return new CuentaNoEncontradaException(CUENTA_NO_ENCONTRADA);
                });

        if (cuenta.getEstatus() != EstatusCuenta.ACTIVA) {
            throw new CuentaBloqueadaException("La cuenta no está activa y no admite operaciones.");
        }
        return cuenta;
    }

    @Override
    @Transactional
    public void validarPinTransaccional(Cuenta cuenta, String pinIngresado) {
        if (pinIngresado == null || pinIngresado.trim().isEmpty()) {
            throw new PinInvalidoException("El PIN de autorización es obligatorio.");
        }

        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.systemDefault());
        verificarBloqueo(cuenta, ahora);

        String hashAValidar = obtenerHashValidacion(cuenta);
        if (passwordEncoder.matches(pinIngresado, hashAValidar)) {
            limpiarIntentosSiAplica(cuenta);
        } else {
            manejarPinFallido(cuenta, ahora);
        }
    }

    private void verificarBloqueo(Cuenta cuenta, OffsetDateTime ahora) {
        if (cuenta.getBloqueoPinHasta() != null && cuenta.getBloqueoPinHasta().isAfter(ahora)) {
            log.warn("Intento de operación con cuenta bloqueada por PIN. cuentaId={}", cuenta.getId());
            throw new PinBloqueadoException(
                    "Operación bloqueada temporalmente por rebasar los intentos permitidos. Intente de nuevo después de " +
                            cuenta.getBloqueoPinHasta()
            );
        }
    }

    private String obtenerHashValidacion(Cuenta cuenta) {
        String pinHash = cuenta.getPinHash();
        if (pinHash != null && !pinHash.isBlank()) {
            return pinHash;
        }
        Usuario usuario = usuarioRepository.findByClienteId(cuenta.getCliente().getId())
                .orElseThrow(() -> new ReglaNegocioException("No se encontró usuario de acceso para la cuenta."));
        return usuario.getPassword();
    }

    private void limpiarIntentosSiAplica(Cuenta cuenta) {
        if ((cuenta.getIntentosFallidosPin() != null && cuenta.getIntentosFallidosPin() > 0)
                || cuenta.getBloqueoPinHasta() != null) {
            cuenta.setIntentosFallidosPin(0);
            cuenta.setBloqueoPinHasta(null);
            cuentaRepository.save(cuenta);
        }
    }

    private void manejarPinFallido(Cuenta cuenta, OffsetDateTime ahora) {
        int intentos = (cuenta.getIntentosFallidosPin() == null ? 0 : cuenta.getIntentosFallidosPin()) + 1;
        cuenta.setIntentosFallidosPin(intentos);

        if (intentos >= MAX_INTENTOS_PIN) {
            cuenta.setBloqueoPinHasta(ahora.plusMinutes(MINUTOS_BLOQUEO));
            cuentaRepository.save(cuenta);
            log.warn("Cuenta bloqueada por rebasar intentos de PIN/credencial. cuentaId={}, bloqueoHasta={}",
                    cuenta.getId(), cuenta.getBloqueoPinHasta());
            throw new PinBloqueadoException(String.format(
                    "Ha superado los %d intentos permitidos. Su PIN transaccional ha sido bloqueado por %d minutos.",
                    MAX_INTENTOS_PIN, MINUTOS_BLOQUEO
            ));
        }

        cuentaRepository.save(cuenta);
        int restantes = MAX_INTENTOS_PIN - intentos;
        throw new PinInvalidoException(String.format(
                "El PIN de autorización es incorrecto. Intentos restantes antes de bloqueo: %d.",
                restantes
        ));
    }
}
