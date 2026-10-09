package com.proyecto.servicios.model.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO de respuesta para exponer la información del cliente sin exponer
 * directamente la entidad JPA (evita problemas de serialización lazy y
 * expone solo los datos necesarios).
 */
@Data
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

    // Datos de domicilio anidados
    private DomicilioDTO domicilio;

    // Solo los números de cuenta para no generar referencias circulares
    private List<String> numeroCuentas;

    /**
     * Convierte una entidad Cliente y su lista de cuentas en un ClienteResponse.
     * Se recibe la lista de cuentas de forma explícita para evitar accesos lazy fuera de transacción.
     *
     * @param cliente entidad persistida
     * @param cuentas lista de cuentas asociadas al cliente
     * @return DTO listo para serializar
     */
    public static ClienteResponse from(Cliente cliente, List<Cuenta> cuentas) {
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

        // Mapear domicilio si existe
        if (cliente.getDomicilio() != null) {
            DomicilioDTO dom = new DomicilioDTO();
            dom.setCalle(cliente.getDomicilio().getCalle());
            dom.setNumeroExterior(cliente.getDomicilio().getNumeroExterior());
            dom.setNumeroInterior(cliente.getDomicilio().getNumeroInterior());
            dom.setColonia(cliente.getDomicilio().getColonia());
            dom.setMunicipio(cliente.getDomicilio().getMunicipio());
            dom.setEstado(cliente.getDomicilio().getEstado());
            dom.setCodigoPostal(cliente.getDomicilio().getCodigoPostal());
            dom.setPais(cliente.getDomicilio().getPais());
            r.setDomicilio(dom);
        }

        // Mapear números de cuenta
        if (cuentas != null) {
            r.setNumeroCuentas(cuentas.stream()
                    .map(Cuenta::getNumeroCuenta)
                    .collect(Collectors.toList()));
        }

        return r;
    }
}
