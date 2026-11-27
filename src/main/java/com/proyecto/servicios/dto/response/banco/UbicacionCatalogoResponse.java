package com.proyecto.servicios.dto.response.banco;

import com.proyecto.servicios.entity.catalogos.CodigoPostalCatalogo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionCatalogoResponse {
    private String estado;
    private String municipio;
    private String codigoPostal;

    public static UbicacionCatalogoResponse desdeEntidad(CodigoPostalCatalogo cp) {
        return new UbicacionCatalogoResponse(
                cp.getEstado() != null ? cp.getEstado().getNombre() : "",
                cp.getMunicipio(),
                cp.getCodigo()
        );
    }
}
