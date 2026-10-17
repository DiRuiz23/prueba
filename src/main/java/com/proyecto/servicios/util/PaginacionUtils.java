package com.proyecto.servicios.util;

import com.proyecto.servicios.dto.request.comun.PaginacionRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Convierte los parámetros de paginación del cliente en un {@link Pageable} de Spring Data.
 */
public final class PaginacionUtils {

    private PaginacionUtils() {
    }

    /**
     * @param paginacion parámetros enviados por el cliente (puede ser {@code null})
     * @param ordenarPor campo de ordenamiento ya validado contra la lista blanca del DTO (puede ser {@code null})
     * @param ordenPorDefecto campo usado cuando el cliente no indica ordenamiento
     */
    public static Pageable crearPageable(PaginacionRequest paginacion, String ordenarPor, String ordenPorDefecto) {
        PaginacionRequest p = paginacion != null ? paginacion : new PaginacionRequest();
        int pagina = p.getPagina() != null ? p.getPagina() : PaginacionRequest.PAGINA_DEFAULT;
        int tamanio = p.getTamanio() != null ? p.getTamanio() : PaginacionRequest.TAMANIO_DEFAULT;
        Sort.Direction direccion = "DESC".equalsIgnoreCase(p.getDireccion())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        String campo = (ordenarPor == null || ordenarPor.isBlank()) ? ordenPorDefecto : ordenarPor;
        return PageRequest.of(pagina, tamanio, Sort.by(direccion, campo));
    }
}
