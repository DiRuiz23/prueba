package com.proyecto.servicios.service.gestopago.impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.model.GestoPagoAuthResponse;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.gestopago.ProveedorNoDisponibleException;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.repository.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.gestopago.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    private final ReentrantLock renewalLock = new ReentrantLock();

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
    }

    /**
     * Según la especificación oficial de GestoPago, un token es válido durante un día entero.
     * Se programa su renovación cada 23 horas (82,800,000 ms) para evitar consultas innecesarias.
     */
    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:82800000}", initialDelay = 10000)
    public void renovarToken() {
        log.info("Ejecutando renovación programada de token GestoPago para distribuidor={}", idDistribuidor);
        try {
            ejecutarAutenticacionYGuardar();
        } catch (Exception e) {
            log.error("Error en renovación programada de token GestoPago: {}", e.getMessage());
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
    }

    @Override
    public String obtenerTokenBearerValido() {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .map(GestoPagoToken::getToken)
                .filter(t -> !t.isBlank())
                .orElseGet(this::forzarRenovacionYObtenerToken);
    }

    @Override
    public String forzarRenovacionYObtenerToken() {
        renewalLock.lock();
        try {
            log.info("Renovación de token GestoPago solicitada (single-flight lock)");
            GestoPagoToken token = ejecutarAutenticacionYGuardar();
            return token.getToken();
        } finally {
            renewalLock.unlock();
        }
    }

    private GestoPagoToken ejecutarAutenticacionYGuardar() {
        GestoPagoAuthResponse response;
        try {
            response = gestoPagoAuthClient.authenticate(idDistribuidor, codigoDispositivo, password);
        } catch (Exception e) {
            log.error("Fallo de comunicación al autenticar con GestoPago: {}", e.getMessage());
            throw new ProveedorNoDisponibleException("No fue posible autenticar con la pasarela de GestoPago.", e);
        }

        if (response == null || response.getToken() == null || response.getToken().isBlank()) {
            log.error("La pasarela de GestoPago no retornó un token de autenticación válido");
            throw new ProveedorNoDisponibleException("Respuesta de autenticación vacía del proveedor GestoPago.");
        }

        GestoPagoToken tokenEntity = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .map(existing -> {
                    tokenMapper.updateEntity(response, existing);
                    return existing;
                })
                .orElseGet(() -> {
                    GestoPagoToken nuevo = tokenMapper.toEntity(response);
                    nuevo.setIdDistribuidor(idDistribuidor);
                    nuevo.setCodigoDispositivo(codigoDispositivo);
                    nuevo.setActivo(true);
                    return nuevo;
                });

        tokenEntity = tokenRepository.save(tokenEntity);
        log.info("Token GestoPago guardado exitosamente en base de datos.");
        return tokenEntity;
    }
}
