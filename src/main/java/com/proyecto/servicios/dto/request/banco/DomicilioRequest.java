package com.proyecto.servicios.dto.request.banco;

import com.proyecto.servicios.dto.request.comun.PatronesValidacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Domicilio completo requerido durante el registro del cliente.
 */
@Data
public class DomicilioRequest {

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle no debe exceder los 100 caracteres")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 20, message = "El número exterior no debe exceder los 20 caracteres")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior no debe exceder los 20 caracteres")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia no debe exceder los 100 caracteres")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    @Size(max = 100, message = "El municipio no debe exceder los 100 caracteres")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 100, message = "El estado no debe exceder los 100 caracteres")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = PatronesValidacion.CODIGO_POSTAL, message = "El código postal debe contener exactamente 5 dígitos numéricos")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 100, message = "El país no debe exceder los 100 caracteres")
    private String pais;
}
