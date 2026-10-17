package com.proyecto.servicios.dto.request.persona;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaIdRequest {

    private Integer id;

    private String nombre;

    @jakarta.validation.constraints.AssertTrue(message = "Debe proporcionar el ID o el nombre de la persona a eliminar")
    public boolean isIdentificadorPresente() {
        return (id != null && id > 0) || (nombre != null && !nombre.isBlank());
    }
}
