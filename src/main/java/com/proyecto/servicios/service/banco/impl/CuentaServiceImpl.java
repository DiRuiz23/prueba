package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.dto.request.banco.*;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.CuentaResponse;
import com.proyecto.servicios.dto.response.banco.SaldoResponse;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.banco.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.banco.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.repository.banco.ClienteRepository;
import com.proyecto.servicios.repository.banco.CuentaRepository;
import com.proyecto.servicios.repository.specification.CuentaSpecification;
import com.proyecto.servicios.service.banco.CuentaService;
import com.proyecto.servicios.util.PaginacionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

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
    public CuentaResponse consultarPorNumeroCuenta(NumeroCuentaRequest request) {
        String num = request.getNumeroCuenta().trim();
        return cuentaRepository.findByNumeroCuenta(num)
                .map(CuentaResponse::desdeEntidad)
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró la cuenta con número: " + num));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<CuentaResponse> consultar(CuentaFiltroRequest request) {
        Specification<Cuenta> spec = Specification.allOf(
                CuentaSpecification.numeroCuentaIgual(request.getNumeroCuenta()),
                CuentaSpecification.clienteIgual(request.getClienteId()),
                CuentaSpecification.estatusIgual(request.getEstatus()),
                CuentaSpecification.saldoMinimo(request.getSaldoMinimo()),
                CuentaSpecification.saldoMaximo(request.getSaldoMaximo())
        );

        Pageable pageable = PaginacionUtils.crearPageable(
                request.getPaginacion(), request.getOrdenarPor(), "id");

        Page<Cuenta> pagina = cuentaRepository.findAll(spec, pageable);
        return PaginaResponse.desdePagina(pagina, CuentaResponse::desdeEntidad);
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
            throw new ReglaNegocioException("No se pueden abrir cuentas para un cliente dado de baja.");
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
                .estatus(EstatusCuenta.ACTIVA)
                .build();

        cuenta = cuentaRepository.save(cuenta);
        log.info("Cuenta adicional creada: {} para cliente ID: {}", cuenta.getNumeroCuenta(), cliente.getId());
        return CuentaResponse.desdeEntidad(cuenta);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTUALIZACIÓN DE ESTATUS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CuentaResponse cambiarEstatusCuenta(ActualizarEstatusCuentaRequest request) {
        String num = request.getNumeroCuenta().trim();
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(num)
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró la cuenta con número: " + num));

        // Regla: no se puede activar la cuenta de un cliente dado de baja
        if (request.getNuevoEstatus() == EstatusCuenta.ACTIVA && !cuenta.getCliente().getActivo()) {
            throw new ReglaNegocioException("No se puede activar una cuenta perteneciente a un cliente inactivo.");
        }

        cuenta.setEstatus(request.getNuevoEstatus());
        cuenta = cuentaRepository.save(cuenta);
        log.info("Estatus de la cuenta {} actualizado a {}", num, request.getNuevoEstatus());
        return CuentaResponse.desdeEntidad(cuenta);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SALDO
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SaldoResponse consultarSaldo(NumeroCuentaRequest request) {
        String num = request.getNumeroCuenta().trim();
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(num)
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró la cuenta con número: " + num));
        return new SaldoResponse(cuenta.getNumeroCuenta(), cuenta.getSaldo());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVADO
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