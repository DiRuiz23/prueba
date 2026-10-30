package com.proyecto.servicios.service.banco.strategy;

import com.proyecto.servicios.client.model.CatProductXml.ProductoItem;

import java.math.BigDecimal;

public interface ReglaMontoStrategy {

    boolean soporta(Integer tipoFront);

    ValidacionMontoResult validarYCalcular(BigDecimal montoRequest, ProductoItem producto);

    void validarReferencia(String telefonoReferencia, ProductoItem producto);
}

