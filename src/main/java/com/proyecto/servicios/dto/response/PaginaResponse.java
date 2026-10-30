package com.proyecto.servicios.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Representación estable de una página de resultados. Evita exponer
 * directamente la estructura interna de {@link Page} de Spring Data.
 *
 * @param <T> tipo de los elementos de la página
 */
@Getter
@Setter
@NoArgsConstructor
public class PaginaResponse<T> {

    private List<T> contenido;
    private int pagina;
    private int tamanio;
    private long totalElementos;
    private int totalPaginas;
    private boolean ultima;

    /**
     * Construye la respuesta a partir de una página de entidades, convirtiendo cada elemento a su DTO.
     */
    public static <E, T> PaginaResponse<T> desdePagina(Page<E> pagina, Function<E, T> convertidor) {
        PaginaResponse<T> respuesta = new PaginaResponse<>();
        respuesta.setContenido(pagina.getContent().stream().map(convertidor).toList());
        respuesta.setPagina(pagina.getNumber());
        respuesta.setTamanio(pagina.getSize());
        respuesta.setTotalElementos(pagina.getTotalElements());
        respuesta.setTotalPaginas(pagina.getTotalPages());
        respuesta.setUltima(pagina.isLast());
        return respuesta;
    }
}
