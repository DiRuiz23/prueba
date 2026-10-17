package com.proyecto.servicios.dto.response.banco;

import com.proyecto.servicios.entity.banco.Domicilio;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DomicilioResponse {

    private String calle;
    private String numeroExterior;
    private String numeroInterior;
    private String colonia;
    private String municipio;
    private String estado;
    private String codigoPostal;
    private String pais;

    public static DomicilioResponse desdeEntidad(Domicilio d) {
        if (d == null) {
            return null;
        }
        DomicilioResponse r = new DomicilioResponse();
        r.setCalle(d.getCalle());
        r.setNumeroExterior(d.getNumeroExterior());
        r.setNumeroInterior(d.getNumeroInterior());
        r.setColonia(d.getColonia());
        r.setMunicipio(d.getMunicipio());
        r.setEstado(d.getEstado());
        r.setCodigoPostal(d.getCodigoPostal());
        r.setPais(d.getPais());
        return r;
    }
}
