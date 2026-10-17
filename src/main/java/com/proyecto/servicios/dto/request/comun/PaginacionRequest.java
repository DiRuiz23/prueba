package com.proyecto.servicios.dto.request.comun;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parámetros de paginación recibidos en el cuerpo de las consultas.
 * Todos los campos son opcionales y tienen valores por defecto seguros.
 */
@Getter
@Setter
@NoArgsConstructor
public class PaginacionRequest {

    public static final int PAGINA_DEFAULT = 0;
    public static final int TAMANIO_DEFAULT = 20;
    public static final int TAMANIO_MAXIMO = 100;

    @Min(value = 0, message = "La página debe ser mayor o igual a 0")
    private Integer pagina = PAGINA_DEFAULT;

    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Max(value = TAMANIO_MAXIMO, message = "El tamaño de página no puede exceder " + TAMANIO_MAXIMO + " registros")
    private Integer tamanio = TAMANIO_DEFAULT;

    @Pattern(regexp = "^(?i)(ASC|DESC)$", message = "La dirección de ordenamiento debe ser 'ASC' o 'DESC'")
    private String direccion = "ASC";
}
