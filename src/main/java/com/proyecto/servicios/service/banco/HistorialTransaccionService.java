package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.dto.response.banco.PagoServicioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HistorialTransaccionService {

    /**
     * Consulta el historial paginado de compras y pagos exitosos para el usuario autenticado.
     */
    Page<PagoServicioResponse> obtenerHistorialExitoso(String correoAutenticado, Pageable pageable);
}

