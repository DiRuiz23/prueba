package com.proyecto.servicios.dto.response.persona;

import com.proyecto.servicios.entity.persona.Personas;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaResponse {

    private Integer id;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;

    public static PersonaResponse desdeEntidad(Personas p) {
        if (p == null) {
            return null;
        }
        PersonaResponse r = new PersonaResponse();
        r.setId(p.getId());
        r.setNombre(p.getNombre());
        r.setApellidoPaterno(p.getApellidoPaterno());
        r.setApellidoMaterno(p.getApellidoMaterno());
        return r;
    }

    public String getApellidoP() {
        return apellidoPaterno;
    }

    public void setApellidoP(String apellidoP) {
        this.apellidoPaterno = apellidoP;
    }
}
