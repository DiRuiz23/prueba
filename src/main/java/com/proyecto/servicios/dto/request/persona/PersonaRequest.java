package com.proyecto.servicios.dto.request.persona;

import com.proyecto.servicios.dto.request.comun.PatronesValidacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PersonaRequest {

    private Integer id;

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = PatronesValidacion.NOMBRE, message = "El nombre solo debe contener letras y tener entre 2 y 50 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = PatronesValidacion.NOMBRE, message = "El apellido paterno solo debe contener letras y tener entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = PatronesValidacion.NOMBRE, message = "El apellido materno solo debe contener letras y tener entre 2 y 50 caracteres")
    private String apellidoMaterno;

    // Métodos getter/setter de conveniencia para compatibilidad con código legado que usaba apellidoP
    public String getApellidoP() {
        return apellidoPaterno;
    }

    public void setApellidoP(String apellidoP) {
        this.apellidoPaterno = apellidoP;
    }
}
