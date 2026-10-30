package com.proyecto.servicios.dto.response.banco;

import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO de respuesta para Cuenta bancaria.
 */
@Data
@NoArgsConstructor
public class CuentaResponse {

    private Long id;
    private Long clienteId;
    private String nombreCliente;
    private String numeroCuenta;
    private BigDecimal saldo;
    private EstatusCuenta estatus;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;

    public static CuentaResponse desdeEntidad(Cuenta cuenta) {
        if (cuenta == null) {
            return null;
        }
        CuentaResponse r = new CuentaResponse();
        r.setId(cuenta.getId());
        r.setNumeroCuenta(cuenta.getNumeroCuenta());
        r.setSaldo(cuenta.getSaldo());
        r.setEstatus(cuenta.getEstatus());
        r.setFechaCreacion(cuenta.getFechaCreacion());
        r.setFechaActualizacion(cuenta.getFechaActualizacion());

        if (cuenta.getCliente() != null) {
            r.setClienteId(cuenta.getCliente().getId());
            r.setNombreCliente(cuenta.getCliente().getNombre() + " "
                    + cuenta.getCliente().getApellidoPaterno());
        }

        return r;
    }
}
