package com.proyecto.servicios.model.banco;

import com.proyecto.servicios.entity.banco.Cuenta;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO de respuesta para Cuenta bancaria. Evita serializar la entidad JPA
 * directamente e incluye solo el clienteId en lugar del objeto Cliente completo.
 */
@Data
public class CuentaResponse {

    private Long id;
    private Long clienteId;
    private String nombreCliente;
    private String numeroCuenta;
    private BigDecimal saldo;
    private Cuenta.EstatusCuenta estatus;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;

    /**
     * Convierte una entidad Cuenta en un CuentaResponse.
     *
     * @param cuenta entidad persistida
     * @return DTO listo para serializar
     */
    public static CuentaResponse from(Cuenta cuenta) {
        CuentaResponse r = new CuentaResponse();
        r.setId(cuenta.getId());
        r.setNumeroCuenta(cuenta.getNumeroCuenta());
        r.setSaldo(cuenta.getSaldo());
        r.setEstatus(cuenta.getEstatus());
        r.setFechaCreacion(cuenta.getFechaCreacion());
        r.setFechaActualizacion(cuenta.getFechaActualizacion());

        // Incluir info básica del cliente sin serializar el objeto completo
        if (cuenta.getCliente() != null) {
            r.setClienteId(cuenta.getCliente().getId());
            r.setNombreCliente(cuenta.getCliente().getNombre() + " "
                    + cuenta.getCliente().getApellidoPaterno());
        }

        return r;
    }
}
