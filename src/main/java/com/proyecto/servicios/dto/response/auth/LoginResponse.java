package com.proyecto.servicios.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tipo;
    private String correo;

    @Builder.Default
    private Boolean requiere2fa = false;

    private String mensaje;

    @Builder.Default
    private Boolean sesionActivaDetectada = false;

    private String dispositivoSesionActiva;

    private Integer tiempoExpiracionOtpSegundos;

    public LoginResponse(String token, String tipo, String correo) {
        this.token = token;
        this.tipo = tipo;
        this.correo = correo;
        this.requiere2fa = false;
        this.sesionActivaDetectada = false;
    }
}
