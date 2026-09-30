package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class CatProductXml {

    /**
     * Elemento anidado que contiene el código y texto de resultado de la operación.
     */
    @XmlElement(name = "MENSAJE")
    private Mensaje mensaje;

    /**
     * Wrapper del listado de productos. Contiene N elementos {@code <producto>}.
     */
    @XmlElement(name = "PRODUCTOS")
    private ProductosWrapper productosWrapper;

    /**
     * Retorna el código de resultado (ej. "01" = éxito).
     */
    public String getStatus() {
        return mensaje != null ? mensaje.getCodigo() : null;
    }

    /**
     * Retorna el texto descriptivo del resultado.
     */
    public String getMessage() {
        return mensaje != null ? mensaje.getTexto() : null;
    }

    /**
     * Retorna la lista plana de productos para uso en el servicio.
     */
    @JsonProperty("products")
    public List<ProductoItem> getProducts() {
        return productosWrapper != null ? productosWrapper.getProductos() : null;
    }

    // =========================================================================
    // Clases internas de soporte
    // =========================================================================

    /**
     * Elemento {@code <MENSAJE>} con código y texto del resultado de la operación.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Mensaje {

        @XmlElement(name = "CODIGO")
        private String codigo;

        @XmlElement(name = "TEXTO")
        private String texto;
    }

    /**
     * Elemento wrapper {@code <PRODUCTOS>} que contiene la lista de
     * {@code <producto>}.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProductosWrapper {

        @XmlElement(name = "producto")
        private List<ProductoItem> productos;
    }

    /**
     * Representa un producto individual del catálogo de GestoPago.
     *
     * Los campos son ATRIBUTOS del elemento XML {@code <producto>},
     * por eso se usa {@code XmlAccessType.NONE} y {@code @XmlAttribute}.
     *
     * Campos:
     * - producto : nombre del producto
     * - servicio : nombre del servicio
     * - idServicio : identificador del servicio
     * - idProducto : identificador del producto
     * - idCatTipoServicio : identificador del tipo de servicio
     * - tipoFront : tipo de payload requerido por el front
     * - hasDigitoVerificador: si el producto requiere dígito verificador
     * - precio : precio final del producto
     * - showAyuda : si se debe mostrar ayuda
     * - tipoReferencia : @Deprecated, reemplazado por tipoFront
     * - legend : texto informativo (elemento hijo CDATA)
     */
    @XmlAccessorType(XmlAccessType.NONE)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProductoItem {

        @XmlAttribute(name = "producto")
        private String producto;

        @XmlAttribute(name = "servicio")
        private String servicio;

        @XmlAttribute(name = "idServicio")
        private Integer idServicio;

        @XmlAttribute(name = "idProducto")
        private Integer idProducto;

        @XmlAttribute(name = "idCatTipoServicio")
        private Integer idCatTipoServicio;

        @XmlAttribute(name = "tipoFront")
        private Integer tipoFront;

        @XmlAttribute(name = "hasDigitoVerificador")
        private Boolean hasDigitoVerificador;

        @XmlAttribute(name = "precio")
        private String precio;

        @XmlAttribute(name = "showAyuda")
        private Boolean showAyuda;

        /** @deprecated Según documentación GestoPago: usar tipoFront en su lugar. */
        @Deprecated
        @XmlAttribute(name = "tipoReferencia")
        private String tipoReferencia;

        /** Texto informativo del producto (CDATA). */
        @XmlElement(name = "legend")
        private String legend;
    }
}
