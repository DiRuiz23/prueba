package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.model.banco.ActualizarClientePatchRequest;
import com.proyecto.servicios.model.banco.ClienteResponse;
import com.proyecto.servicios.model.banco.RegistroClienteRequest;

import java.time.OffsetDateTime;
import java.util.List;

public interface ClienteService {

    /** Registra un cliente nuevo con cuenta bancaria, usuario de acceso y biometría. */
    ClienteResponse registrarCliente(RegistroClienteRequest request);

    /** Consulta un cliente por su ID numérico. */
    ClienteResponse consultarPorId(Long id);

    /** Busca un cliente por el número de cuenta bancaria asociada. */
    ClienteResponse consultarPorNumeroCuenta(String numeroCuenta);

    /**
     * Busca clientes aplicando filtros opcionales dinámicos.
     * Si ningún filtro aplica, retorna todos los clientes.
     */
    List<ClienteResponse> buscarConFiltros(
            String nombre,
            String apellidoPaterno,
            String apellidoMaterno,
            String curp,
            String rfc,
            Boolean soloActivos,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin
    );

    /** Aplica una actualización parcial (PATCH) al cliente. CURP y RFC son inmutables. */
    ClienteResponse actualizarClienteParcial(Long id, ActualizarClientePatchRequest patch);

    /** Realiza la baja lógica del cliente, desactivando su usuario y cuentas. */
    void bajaLogicaCliente(Long id);
}
