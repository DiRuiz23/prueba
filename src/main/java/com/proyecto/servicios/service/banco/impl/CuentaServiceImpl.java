package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.exception.banco.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.banco.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.banco.ReglaNegocioException;
import com.proyecto.servicios.model.banco.CrearCuentaRequest;
import com.proyecto.servicios.model.banco.CuentaResponse;
import com.proyecto.servicios.repositorys.banco.ClienteRepository;
import com.proyecto.servicios.repositorys.banco.CuentaRepository;
import com.proyecto.servicios.service.banco.CuentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // CONSULTAS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CuentaResponse consultarPorNumeroCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .map(CuentaResponse::from)
                .orElseThrow(() -> new CuentaNoEncontradaException(
                        "No se encontró la cuenta con número: " + numeroCuenta));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> consultarPorClienteId(Long clienteId) {
        return cuentaRepository.findByClienteId(clienteId)
                .stream()
                .map(CuentaResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> consultarPorEstatus(Cuenta.EstatusCuenta estatus) {
        return cuentaRepository.findByEstatus(estatus)
                .stream()
                .map(CuentaResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> consultarTodas() {
        return cuentaRepository.findAll()
                .stream()
                .map(CuentaResponse::from)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CuentaResponse crearCuentaAdicional(CrearCuentaRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(
                        "No se encontró el cliente con ID: " + request.getClienteId()));

        if (!cliente.getActivo()) {
            throw new ReglaNegocioException(
                    "No se pueden abrir cuentas para un cliente dado de baja.");
        }

        BigDecimal saldoInicial = request.getSaldoInicial() != null
                ? request.getSaldoInicial()
                : BigDecimal.ZERO;

        if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El saldo inicial no puede ser negativo.");
        }

        Cuenta cuenta = Cuenta.builder()
                .cliente(cliente)
                .numeroCuenta(generarNumeroCuenta())
                .saldo(saldoInicial)
                .estatus(Cuenta.EstatusCuenta.ACTIVA)
                .build();

        cuenta = cuentaRepository.save(cuenta);
        log.info("Cuenta adicional creada: {} para cliente ID: {}", cuenta.getNumeroCuenta(), cliente.getId());
        return CuentaResponse.from(cuenta);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTUALIZACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CuentaResponse cambiarEstatusCuenta(String numeroCuenta, Cuenta.EstatusCuenta nuevoEstatus) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(
                        "No se encontró la cuenta con número: " + numeroCuenta));

        // Regla: no se puede activar la cuenta de un cliente dado de baja
        if (nuevoEstatus == Cuenta.EstatusCuenta.ACTIVA && !cuenta.getCliente().getActivo()) {
            throw new ReglaNegocioException(
                    "No se puede activar una cuenta perteneciente a un cliente inactivo.");
        }

        cuenta.setEstatus(nuevoEstatus);
        cuenta = cuentaRepository.save(cuenta);
        log.info("Estatus de la cuenta {} actualizado a {}", numeroCuenta, nuevoEstatus);
        return CuentaResponse.from(cuenta);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SALDO
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BigDecimal consultarSaldo(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .map(Cuenta::getSaldo)
                .orElseThrow(() -> new CuentaNoEncontradaException(
                        "No se encontró la cuenta con número: " + numeroCuenta));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private String generarNumeroCuenta() {
        SecureRandom random = new SecureRandom();
        String numero;
        do {
            long sufijo = 1_000_000_000L + (long) (random.nextDouble() * 9_000_000_000L);
            numero = "012" + sufijo;
        } while (cuentaRepository.existsByNumeroCuenta(numero));
        return numero;
    }
}