package com.proyecto.servicios.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificarOtpRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String correo;

    @NotBlank(message = "El código de verificación es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El código OTP debe ser exactamente de 6 dígitos numéricos")
    private String codigo;

    @Builder.Default
    private String dispositivo = "DESCONOCIDO";

    private String ipOrigen;
}
