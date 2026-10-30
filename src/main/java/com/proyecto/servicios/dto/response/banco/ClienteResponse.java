package com.proyecto.servicios.dto.response.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de respuesta para exponer la información del cliente sin exponer
 * directamente la entidad JPA ni generar referencias circulares.
 */
@Data
@NoArgsConstructor
public class ClienteResponse {

    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private String sexo;
    private String nacionalidad;
    private String estadoCivil;
    private String correo;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;

    private DomicilioResponse domicilio;
    private List<String> numeroCuentas = new ArrayList<>();

    public static ClienteResponse desdeEntidad(Cliente cliente) {
        return desdeEntidad(cliente, cliente.getCuentas());
    }

    public static ClienteResponse desdeEntidad(Cliente cliente, List<Cuenta> cuentas) {
        if (cliente == null) {
            return null;
        }
        ClienteResponse r = new ClienteResponse();
        r.setId(cliente.getId());
        r.setNombre(cliente.getNombre());
        r.setSegundoNombre(cliente.getSegundoNombre());
        r.setApellidoPaterno(cliente.getApellidoPaterno());
        r.setApellidoMaterno(cliente.getApellidoMaterno());
        r.setFechaNacimiento(cliente.getFechaNacimiento());
        r.setCurp(cliente.getCurp());
        r.setRfc(cliente.getRfc());
        r.setSexo(cliente.getSexo());
        r.setNacionalidad(cliente.getNacionalidad());
        r.setEstadoCivil(cliente.getEstadoCivil());
        r.setCorreo(cliente.getCorreo());
        r.setTelefonoMovil(cliente.getTelefonoMovil());
        r.setTelefonoAlternativo(cliente.getTelefonoAlternativo());
        r.setOcupacion(cliente.getOcupacion());
        r.setEmpresa(cliente.getEmpresa());
        r.setIngresoMensual(cliente.getIngresoMensual());
        r.setActivo(cliente.getActivo());
        r.setFechaCreacion(cliente.getFechaCreacion());
        r.setFechaActualizacion(cliente.getFechaActualizacion());

        if (cliente.getDomicilio() != null) {
            r.setDomicilio(DomicilioResponse.desdeEntidad(cliente.getDomicilio()));
        }

        if (cuentas != null) {
            r.setNumeroCuentas(cuentas.stream().map(Cuenta::getNumeroCuenta).toList());
        }

        return r;
    }
}
