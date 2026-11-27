package com.proyecto.servicios.service.banco;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Cliente HTTP con caché en memoria para consultar códigos postales y colonias
 * de todo México mediante la API de Postalia.
 */
@Service
@Slf4j
public class PostaliaCodigoPostalService {

    private static final int MAX_CACHE_SIZE = 5000;
    private static final Duration TTL_ENCONTRADO = Duration.ofHours(24);
    private static final Duration TTL_NO_ENCONTRADO = Duration.ofMinutes(30);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(8);
    private static final Pattern PATRON_CP = Pattern.compile("^\\d{5}$");
    private static final Pattern PATRON_DIACRITICOS = Pattern.compile("\\p{M}+");
    private static final String DEFAULT_BASE_URL = "https://postalia.com.mx/api/codigos-postales";

    private static final List<String> ESTADOS_CANONICOS = List.of(
            "Aguascalientes",
            "Baja California",
            "Baja California Sur",
            "Campeche",
            "Chiapas",
            "Chihuahua",
            "Ciudad de México",
            "Coahuila",
            "Colima",
            "Durango",
            "Estado de México",
            "Guanajuato",
            "Guerrero",
            "Hidalgo",
            "Jalisco",
            "Michoacán",
            "Morelos",
            "Nayarit",
            "Nuevo León",
            "Oaxaca",
            "Puebla",
            "Querétaro",
            "Quintana Roo",
            "San Luis Potosí",
            "Sinaloa",
            "Sonora",
            "Tabasco",
            "Tamaulipas",
            "Tlaxcala",
            "Veracruz",
            "Yucatán",
            "Zacatecas"
    );

    private static final Map<String, String> ALIAS_ESTADOS = construirAliasEstados();

    public record CodigoPostalPostalia(
            String codigoPostal,
            String estado,
            String municipio,
            String ciudad,
            String zona,
            List<String> colonias
    ) {
    }

    private record CacheEntry(CodigoPostalPostalia datos, Instant expiresAt) {
        boolean expirada(Instant ahora) {
            return ahora.isAfter(expiresAt);
        }
    }

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiToken;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public PostaliaCodigoPostalService(
            ObjectMapper objectMapper,
            @Value("${postalia.api.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${postalia.api.token:}") String apiToken) {
        this(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                objectMapper,
                baseUrl,
                apiToken);
    }

    PostaliaCodigoPostalService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            String baseUrl,
            String apiToken) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        String urlLimpia = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl.trim();
        this.baseUrl = urlLimpia.replaceAll("/+$", "");
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    public Optional<CodigoPostalPostalia> consultarCodigoPostal(String codigoPostal) {
        if (codigoPostal == null || apiToken.isEmpty()) {
            return Optional.empty();
        }
        String cp = codigoPostal.trim();
        if (!PATRON_CP.matcher(cp).matches()) {
            return Optional.empty();
        }

        Instant ahora = Instant.now();
        CacheEntry cached = cache.get(cp);
        if (cached != null) {
            if (!cached.expirada(ahora)) {
                return Optional.ofNullable(cached.datos());
            }
            cache.remove(cp, cached);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + cp))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiToken)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();

            if (status == 200) {
                Optional<CodigoPostalPostalia> parseado = parsearRespuesta(cp, response.body());
                parseado.ifPresent(datos ->
                        guardarEnCache(cp, new CacheEntry(datos, ahora.plus(TTL_ENCONTRADO)), ahora));
                return parseado;
            }

            if (status == 404) {
                guardarEnCache(cp, new CacheEntry(null, ahora.plus(TTL_NO_ENCONTRADO)), ahora);
                return Optional.empty();
            }

            log.warn("Postalia respondió HTTP {} para código postal {}", status, cp);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Consulta de código postal {} en Postalia interrumpida: {}", cp, e.getMessage());
            return Optional.empty();
        } catch (IOException e) {
            log.warn("Error de comunicación con Postalia al consultar CP {}: {}", cp, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error inesperado al consultar CP {} en Postalia: {}", cp, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<CodigoPostalPostalia> parsearRespuesta(String cpSolicitado, String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            String cpRespuesta = textoLimpio(root.path("codigo_postal").asText(cpSolicitado));
            String cpFinal = PATRON_CP.matcher(cpRespuesta).matches() ? cpRespuesta : cpSolicitado;
            String estado = normalizarEstado(root.path("estado").asText(""));
            String municipio = textoLimpio(root.path("municipio").asText(""));
            String ciudad = textoLimpio(root.path("ciudad").asText(""));
            String zona = textoLimpio(root.path("zona").asText(""));

            if (estado.isEmpty() || municipio.isEmpty()) {
                return Optional.empty();
            }

            Set<String> coloniasSet = new LinkedHashSet<>();
            JsonNode coloniasNode = root.path("colonias");
            if (coloniasNode.isArray()) {
                for (JsonNode item : coloniasNode) {
                    String nombre = item.isTextual()
                            ? textoLimpio(item.asText())
                            : textoLimpio(item.path("nombre").asText(""));
                    if (!nombre.isEmpty()) {
                        coloniasSet.add(nombre);
                    }
                }
            }

            List<String> colonias = new ArrayList<>(coloniasSet);
            colonias.sort(String.CASE_INSENSITIVE_ORDER);

            return Optional.of(new CodigoPostalPostalia(
                    cpFinal,
                    estado,
                    municipio,
                    ciudad,
                    zona,
                    List.copyOf(colonias)
            ));
        } catch (Exception e) {
            log.warn("No se pudo interpretar la respuesta de Postalia para CP {}: {}", cpSolicitado, e.getMessage());
            return Optional.empty();
        }
    }

    private void guardarEnCache(String cp, CacheEntry entry, Instant ahora) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.entrySet().removeIf(e -> e.getValue().expirada(ahora));
            if (cache.size() >= MAX_CACHE_SIZE) {
                cache.clear();
            }
        }
        cache.put(cp, entry);
    }

    public static String normalizarEstado(String estadoRaw) {
        String limpio = textoLimpio(estadoRaw);
        if (limpio.isEmpty()) {
            return "";
        }
        String clave = normalizarTextoComparacion(limpio);
        return ALIAS_ESTADOS.getOrDefault(clave, limpio);
    }

    public static String normalizarTextoComparacion(String texto) {
        if (texto == null) {
            return "";
        }
        String nfd = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD);
        return PATRON_DIACRITICOS.matcher(nfd)
                .replaceAll("")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static String textoLimpio(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
    }

    private static Map<String, String> construirAliasEstados() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String estado : ESTADOS_CANONICOS) {
            map.put(normalizarTextoComparacion(estado), estado);
        }
        map.put("mexico", "Estado de México");
        map.put("estado de mexico", "Estado de México");
        map.put("edomex", "Estado de México");
        map.put("cdmx", "Ciudad de México");
        map.put("distrito federal", "Ciudad de México");
        map.put("coahuila de zaragoza", "Coahuila");
        map.put("michoacan de ocampo", "Michoacán");
        map.put("veracruz de ignacio de la llave", "Veracruz");
        return Map.copyOf(map);
    }
}
