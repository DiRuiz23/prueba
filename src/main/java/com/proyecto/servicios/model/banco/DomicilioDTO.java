package com.proyecto.servicios.model.banco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DomicilioDTO {
    @NotBlank(message = "La calle es obligatoria")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    private String numeroExterior;

    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos numéricos")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    private String pais;
}
