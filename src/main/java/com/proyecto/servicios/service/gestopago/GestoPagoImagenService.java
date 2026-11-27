package com.proyecto.servicios.service.gestopago;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Obtiene y cachea en memoria las imágenes de servicios desde GestoPago para
 * servirlas desde el backend con cabeceras CORS y evitar fallos de carga en Web/móvil.
 */
@Service
@Slf4j
public class GestoPagoImagenService {

    private static final int MAX_CACHE_SIZE = 1000;
    private static final Duration TTL_ENCONTRADA = Duration.ofHours(24);
    private static final Duration TTL_NO_ENCONTRADA = Duration.ofMinutes(30);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(8);
    private static final String DEFAULT_BASE_URL =
            "https://gestopago.portalventas.net/sistema/images/gestopago/servicios";

    public record ImagenServicio(byte[] bytes, MediaType contentType) {
    }

    private record CacheEntry(ImagenServicio imagen, Instant expiresAt) {
        boolean expirada(Instant ahora) {
            return ahora.isAfter(expiresAt);
        }
    }

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Map<Integer, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public GestoPagoImagenService(
            @Value("${gestopago.imagenes.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), baseUrl);
    }

    GestoPagoImagenService(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        String normalized = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        this.baseUrl = normalized.replaceAll("/+$", "");
    }

    public Optional<ImagenServicio> obtenerImagen(Integer idServicio) {
        if (idServicio == null || idServicio <= 0) {
            return Optional.empty();
        }

        Instant ahora = Instant.now();
        CacheEntry cached = cache.get(idServicio);
        if (cached != null) {
            if (!cached.expirada(ahora)) {
                return Optional.ofNullable(cached.imagen());
            }
            cache.remove(idServicio, cached);
        }

        String url = baseUrl + "/" + idServicio + ".png";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "image/*")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            byte[] body = response.body();

            if (status == 200 && body != null && body.length > 0) {
                Optional<MediaType> contentType = resolverContentType(
                        response.headers().firstValue("Content-Type").orElse(null),
                        body);
                if (contentType.isPresent()) {
                    ImagenServicio img = new ImagenServicio(body, contentType.get());
                    guardarEnCache(idServicio, new CacheEntry(img, ahora.plus(TTL_ENCONTRADA)), ahora);
                    return Optional.of(img);
                }
                log.debug("Respuesta no es una imagen válida para servicio {}: Content-Type={}",
                        idServicio, response.headers().firstValue("Content-Type").orElse("none"));
                return Optional.empty();
            }

            if (status == 404 || status == 410) {
                guardarEnCache(idServicio, new CacheEntry(null, ahora.plus(TTL_NO_ENCONTRADA)), ahora);
                return Optional.empty();
            }

            log.warn("Error HTTP {} al consultar imagen de servicio {}", status, idServicio);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Consulta de imagen interrumpida para servicio {}: {}", idServicio, e.getMessage());
            return Optional.empty();
        } catch (IOException e) {
            log.warn("Error de I/O al obtener imagen de servicio {}: {}", idServicio, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error inesperado al obtener imagen de servicio {}: {}", idServicio, e.getMessage());
            return Optional.empty();
        }
    }

    private void guardarEnCache(Integer idServicio, CacheEntry entry, Instant ahora) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.entrySet().removeIf(e -> e.getValue().expirada(ahora));
            if (cache.size() >= MAX_CACHE_SIZE) {
                cache.clear();
            }
        }
        cache.put(idServicio, entry);
    }

    private Optional<MediaType> resolverContentType(String header, byte[] body) {
        if (header != null && !header.isBlank()) {
            try {
                MediaType parsed = MediaType.parseMediaType(header);
                if ("image".equalsIgnoreCase(parsed.getType())) {
                    return Optional.of(new MediaType(parsed.getType(), parsed.getSubtype()));
                }
                return Optional.empty();
            } catch (InvalidMediaTypeException ignored) {
                // Intentar inferencia por firma binaria si el header vino malformado.
            }
        }
        return esPng(body) ? Optional.of(MediaType.IMAGE_PNG) : Optional.empty();
    }

    private boolean esPng(byte[] body) {
        return body.length >= 8
                && (body[0] & 0xFF) == 0x89
                && body[1] == 0x50
                && body[2] == 0x4E
                && body[3] == 0x47;
    }
}
