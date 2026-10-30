package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.Cuenta;

/**
 * Autorización previa a cualquier operación financiera: verifica que el solicitante autenticado
 * sea el titular de la cuenta, que tanto él como la cuenta estén en condiciones de operar y
 * valida el PIN transaccional con control de bloqueo temporal por intentos fallidos.
 */
public interface AutorizacionOperacionService {

    /**
     * Devuelve la cuenta indicada solo si pertenece al usuario autenticado y puede operar.
     *
     * <p>Si la cuenta no existe <b>o pertenece a otro cliente</b> se lanza la misma excepción
     * (404) con el mismo mensaje, para no revelar la existencia de cuentas ajenas.</p>
     *
     * @param numeroCuenta      cuenta sobre la que se desea operar
     * @param correoAutenticado identidad tomada del token JWT (nunca del cuerpo de la petición)
     */
    Cuenta obtenerCuentaAutorizada(String numeroCuenta, String correoAutenticado);

    /**
     * Valida el PIN de seguridad o autorización transaccional de la cuenta.
     * Aplica control de intentos fallidos (máximo 3) y bloqueo por 15 minutos.
     *
     * @param cuenta       cuenta bancaria sobre la que se aplica el débito
     * @param pinIngresado PIN enviado en la solicitud
     */
    void validarPinTransaccional(Cuenta cuenta, String pinIngresado);
}
