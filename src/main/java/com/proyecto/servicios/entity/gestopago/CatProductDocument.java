package com.proyecto.servicios.entity.gestopago;

import com.proyecto.servicios.model.gestopago.CatProductXml;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Documento MongoDB que persiste el catálogo de productos obtenido de GestoPago.
 * Se almacena en la colección "cat_product".
 */
@Document(collection = "cat_product")
@Getter
@Setter
public class CatProductDocument {

    @Id
    private String id;

    @Field("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Field("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Field("fuente")
    private String fuente = "GESTOPAGO";

    @Field("status")
    private String status;

    @Field("products")
    private List<CatProductXml.ProductoItem> products;
}
