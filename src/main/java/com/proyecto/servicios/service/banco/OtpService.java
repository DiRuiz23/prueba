package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.Usuario;

public interface OtpService {

    /**
     * Genera un nuevo código OTP seguro de 6 dígitos para el usuario, lo hashea con SHA-256,
     * lo almacena en BD e intenta enviarlo por correo.
     * Retorna el tiempo de validez en segundos (ej. 300 segundos = 5 min).
     */
    int generarYEnviarOtp(Usuario usuario, String proposito);

    /**
     * Valida el código OTP provisto por el usuario contra el hash en base de datos.
     * Controla intentos fallidos, expiración y uso único.
     */
    boolean validarOtp(Usuario usuario, String codigoPlano, String proposito);
}

