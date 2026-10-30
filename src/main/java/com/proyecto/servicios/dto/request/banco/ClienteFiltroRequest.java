package com.proyecto.servicios.dto.request.banco;

import com.proyecto.servicios.dto.request.comun.PaginacionRequest;
import com.proyecto.servicios.dto.request.comun.PatronesValidacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

/**
 * Criterios de búsqueda dinámica para clientes recibidos vía @RequestBody.
 * Permite combinar libremente cualquier filtro.
 */
@Data
public class ClienteFiltroRequest {

    @Size(max = 50, message = "El nombre no debe exceder 50 caracteres")
    private String nombre;

    @Size(max = 50, message = "El apellido paterno no debe exceder 50 caracteres")
    private String apellidoPaterno;

    @Size(max = 50, message = "El apellido materno no debe exceder 50 caracteres")
    private String apellidoMaterno;

    @Size(max = 18, message = "La CURP no debe exceder 18 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "La CURP solo puede contener caracteres alfanuméricos")
    private String curp;

    @Size(max = 13, message = "El RFC no debe exceder 13 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "El RFC solo puede contener caracteres alfanuméricos")
    private String rfc;

    @Size(max = 100, message = "El correo no debe exceder 100 caracteres")
    private String correo;

    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = "El número de cuenta debe tener exactamente 13 dígitos")
    private String numeroCuenta;

    private Boolean soloActivos;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime fechaInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime fechaFin;

    @Pattern(regexp = "^(?i)(id|nombre|apellidoPaterno|fechaCreacion|correo)$",
            message = "Campo de ordenamiento inválido. Permitidos: id, nombre, apellidoPaterno, fechaCreacion, correo")
    private String ordenarPor;

    @Valid
    private PaginacionRequest paginacion = new PaginacionRequest();

    @AssertTrue(message = "La fecha de inicio debe ser anterior o igual a la fecha de fin")
    public boolean isRangoFechasValido() {
        if (fechaInicio != null && fechaFin != null) {
            return !fechaInicio.isAfter(fechaFin);
        }
        return true;
    }
}
