package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.model.banco.CuentaResponse;
import com.proyecto.servicios.model.banco.CrearCuentaRequest;

import java.math.BigDecimal;
import java.util.List;

public interface CuentaService {

    /** Consulta una cuenta por su número único. */
    CuentaResponse consultarPorNumeroCuenta(String numeroCuenta);

    /** Consulta todas las cuentas asociadas a un cliente. */
    List<CuentaResponse> consultarPorClienteId(Long clienteId);

    /** Consulta cuentas filtradas por estatus. */
    List<CuentaResponse> consultarPorEstatus(Cuenta.EstatusCuenta estatus);

    /** Consulta todas las cuentas del sistema. */
    List<CuentaResponse> consultarTodas();

    /** Crea una cuenta bancaria adicional para un cliente activo existente. */
    CuentaResponse crearCuentaAdicional(CrearCuentaRequest request);

    /** Cambia el estatus de una cuenta (ACTIVA, BLOQUEADA, INACTIVA). */
    CuentaResponse cambiarEstatusCuenta(String numeroCuenta, Cuenta.EstatusCuenta nuevoEstatus);

    /** Consulta el saldo disponible de una cuenta. */
    BigDecimal consultarSaldo(String numeroCuenta);
}
