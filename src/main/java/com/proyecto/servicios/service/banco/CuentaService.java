package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.dto.request.banco.*;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.CuentaResponse;
import com.proyecto.servicios.dto.response.banco.SaldoResponse;

public interface CuentaService {

    /** Consulta una cuenta por su número único. */
    CuentaResponse consultarPorNumeroCuenta(NumeroCuentaRequest request);

    /** Consulta cuentas aplicando filtros combinables y paginación. */
    PaginaResponse<CuentaResponse> consultar(CuentaFiltroRequest request);

    /** Crea una cuenta bancaria adicional para un cliente activo existente. */
    CuentaResponse crearCuentaAdicional(CrearCuentaRequest request);

    /** Cambia el estatus de una cuenta (ACTIVA, BLOQUEADA, INACTIVA). */
    CuentaResponse cambiarEstatusCuenta(ActualizarEstatusCuentaRequest request);

    /** Consulta el saldo disponible de una cuenta. */
    SaldoResponse consultarSaldo(NumeroCuentaRequest request);
}
