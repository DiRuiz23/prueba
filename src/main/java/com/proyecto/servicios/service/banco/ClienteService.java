package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.dto.request.banco.*;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.ClienteResponse;
import com.proyecto.servicios.dto.response.banco.DisponibilidadRegistroResponse;

public interface ClienteService {

    /** Registra un cliente nuevo con cuenta bancaria, usuario de acceso y biometría. */
    ClienteResponse registrarCliente(RegistroClienteRequest request);

    /** Verifica si la CURP, el RFC y el correo están disponibles antes de finalizar el registro. */
    DisponibilidadRegistroResponse verificarDisponibilidad(String curp, String rfc, String correo);

    /** Verifica si la CURP, el RFC, el correo y el teléfono móvil están disponibles antes de finalizar el registro. */
    DisponibilidadRegistroResponse verificarDisponibilidad(String curp, String rfc, String correo, String telefonoMovil);

    /** Consulta un cliente por su ID numérico. */
    ClienteResponse consultarPorId(ClienteIdRequest request);

    /** Busca un cliente por el número de cuenta bancaria asociada. */
    ClienteResponse consultarPorNumeroCuenta(NumeroCuentaRequest request);

    /**
     * Búsqueda dinámica paginada aplicando los criterios enviados en el cuerpo.
     */
    PaginaResponse<ClienteResponse> consultar(ClienteFiltroRequest request);

    /** Aplica una actualización a los campos modificables del cliente. CURP y RFC son inmutables. */
    ClienteResponse actualizarCliente(ActualizarClienteRequest request);

    /** Realiza la baja lógica del cliente, desactivando su usuario y cuentas asociadas. */
    void bajaLogicaCliente(ClienteIdRequest request);
}
