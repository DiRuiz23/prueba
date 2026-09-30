package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductosClient;
import com.proyecto.servicios.entity.gestopago.CatProductDocument;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.enums.GestoPagoEnum;
import com.proyecto.servicios.model.gestopago.CatProductXml;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.repositorys.gestopago.CatProductRepository;
import com.proyecto.servicios.service.GestoPagoProductosService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementación del servicio de catálogo de productos de GestoPago.
 *
 * Responsabilidades:
 *  - Actualizar el catálogo diariamente a las 6:00 AM vía Cron (obtiene XML, parsea con JAXB, persiste en MongoDB).
 *  - Exponer el catálogo almacenado para su consulta bajo demanda.
 */
@Service
@Slf4j
public class GestoPagoProductosServiceImpl implements GestoPagoProductosService {

    private final GestoPagoProductosClient gestoPagoProductosClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final CatProductRepository catProductRepository;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    public GestoPagoProductosServiceImpl(GestoPagoProductosClient gestoPagoProductosClient,
                                         GestoPagoTokenService gestoPagoTokenService,
                                         CatProductRepository catProductRepository) {
        this.gestoPagoProductosClient = gestoPagoProductosClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.catProductRepository = catProductRepository;
    }

    /**
     * Actualiza el catálogo de productos todos los días a las 6:00 AM.
     * Obtiene el XML desde GestoPago, lo parsea con JAXB y persiste en MongoDB.
     */
    @Scheduled(cron = "${gestopago.productos.cron:0 0 6 * * *}")
    public void actualizarCatalogoProductos() {
        log.info("Iniciando actualización de catálogo de productos GestoPago para distribuidor={}", idDistribuidor);

        Optional<GestoPagoToken> tokenOptional = gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);

        if (tokenOptional.isEmpty()) {
            log.error("Actualización cancelada: {}", GestoPagoEnum.TOKEN_NO_ENCONTRADO.getMensaje());
            return;
        }

        String bearerToken = "Bearer " + tokenOptional.get().getToken();

        try {
            String xmlResponse = gestoPagoProductosClient.getProductList(bearerToken);

            if (xmlResponse == null || xmlResponse.isBlank()) {
                log.error("Actualización cancelada: {}", GestoPagoEnum.RESPUESTA_INVALIDA.getMensaje());
                return;
            }

            CatProductXml catProductXml = parsearXml(xmlResponse);

            int totalProductos = catProductXml.getProducts() != null ? catProductXml.getProducts().size() : 0;
            log.info("Catálogo parseado: codigo={}, productos={}", catProductXml.getStatus(), totalProductos);

            CatProductDocument documento = new CatProductDocument();
            documento.setProducts(catProductXml.getProducts());
            documento.setStatus(catProductXml.getStatus());
            documento.setFechaCreacion(LocalDateTime.now());
            documento.setFechaActualizacion(LocalDateTime.now());
            documento.setFuente("GESTOPAGO");

            catProductRepository.save(documento);

            log.info("Catálogo de productos GestoPago actualizado correctamente");

        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            log.error("{}: {}", GestoPagoEnum.ERROR_AUTENTICACION.getMensaje(), e.getMessage(), e);
        } catch (FeignException e) {
            log.error("{} - Status HTTP {}: {}", GestoPagoEnum.ERROR_SERVICIO_EXTERNO.getMensaje(), e.status(), e.getMessage(), e);
        } catch (JAXBException e) {
            log.error("{}: {}", GestoPagoEnum.ERROR_PARSEO_XML.getMensaje(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("{}: {}", GestoPagoEnum.ERROR_COMUNICACION.getMensaje(), e.getMessage(), e);
        }
    }

    /**
     * Retorna el catálogo de productos más reciente almacenado en MongoDB.
     * Si no existe catálogo, retorna una respuesta con mensaje de error.
     */
    @Override
    public GestoPagoProductListResponse obtenerCatalogoProductos() {
        log.info("Consultando catálogo de productos GestoPago desde MongoDB");

        Optional<CatProductDocument> documentoOptional = catProductRepository.findTopByOrderByFechaActualizacionDesc();

        if (documentoOptional.isEmpty()) {
            log.error("Consulta fallida: {}", GestoPagoEnum.SIN_CATALOGO.getMensaje());
            GestoPagoProductListResponse respuestaError = new GestoPagoProductListResponse();
            respuestaError.setStatus("ERROR");
            respuestaError.setMensaje(GestoPagoEnum.SIN_CATALOGO.getMensaje());
            respuestaError.setFechaConsulta(LocalDateTime.now());
            return respuestaError;
        }

        CatProductDocument documento = documentoOptional.get();
        GestoPagoProductListResponse respuesta = new GestoPagoProductListResponse();
        respuesta.setStatus("OK");
        respuesta.setMensaje("Catálogo obtenido correctamente");
        respuesta.setFechaConsulta(documento.getFechaActualizacion());
        respuesta.setProducts(documento.getProducts());

        log.info("Catálogo de productos GestoPago consultado correctamente");
        return respuesta;
    }

    /**
     * Parsea el XML de GestoPago usando JAXB (jakarta.xml.bind).
     */
    private CatProductXml parsearXml(String xml) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(CatProductXml.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (CatProductXml) unmarshaller.unmarshal(new StringReader(xml));
    }
}
