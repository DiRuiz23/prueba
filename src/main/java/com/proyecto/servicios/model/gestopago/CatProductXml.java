package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Modelo JAXB para deserializar la respuesta XML del endpoint getProductList.do de GestoPago.
 *
 * El elemento raíz "response" asume que la respuesta envuelve la lista de productos.
 * Ajustar si el XML real utiliza un nombre de elemento raíz distinto.
 */
@XmlRootElement(name = "response")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class CatProductXml {

    @XmlElement(name = "status")
    private String status;

    @XmlElement(name = "message")
    private String message;

    @XmlElement(name = "product")
    @JsonProperty("products")
    private List<ProductoItem> products;

    /**
     * Representa un producto individual dentro del catálogo de GestoPago.
     *
     * Campos según documentación oficial de getProductList:
     *   - producto         : nombre del producto
     *   - servicio         : nombre del servicio
     *   - idServicio       : identificador del servicio
     *   - idProducto       : identificador del producto
     *   - idCatTipoServicio: identificador del tipo de servicio
     *   - tipoFront        : tipo de payload requerido
     *   - hasDigitoVerificador: si requiere dígito verificador
     *   - precio           : precio final del producto (string, hasta 15 chars)
     *   - showAyuda        : si requiere ayuda de verificación
     *
     * Campos marcados como @Deprecated en la documentación (se incluyen por compatibilidad):
     *   - tipoReferencia   : tipo de payload (reemplazado por tipoFront)
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProductoItem {

        @XmlElement(name = "producto")
        private String producto;

        @XmlElement(name = "servicio")
        private String servicio;

        @XmlElement(name = "idServicio")
        private Integer idServicio;

        @XmlElement(name = "idProducto")
        private Integer idProducto;

        @XmlElement(name = "idCatTipoServicio")
        private Integer idCatTipoServicio;

        @XmlElement(name = "tipoFront")
        private Integer tipoFront;

        @XmlElement(name = "hasDigitoVerificador")
        private Boolean hasDigitoVerificador;

        @XmlElement(name = "precio")
        private String precio;

        @XmlElement(name = "showAyuda")
        private Boolean showAyuda;

        /**
         * @deprecated Según documentación de GestoPago: "Will be deleted on next version".
         * Usar tipoFront en su lugar.
         */
        @Deprecated
        @XmlElement(name = "tipoReferencia")
        private String tipoReferencia;
    }
}
