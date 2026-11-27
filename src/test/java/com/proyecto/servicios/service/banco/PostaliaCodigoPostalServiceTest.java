package com.proyecto.servicios.service.banco;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostaliaCodigoPostalServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private HttpClient httpClient;

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int status, String body) {
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(status);
        if (status == 200) {
            when(resp.body()).thenReturn(body);
        }
        return resp;
    }

    @Test
    @DisplayName("consultarCodigoPostal: normaliza estados oficiales de SEPOMEX, ordena colonias y cachea")
    void consultarCodigoPostal_cuando200_normalizaEstadoYCachea() throws Exception {
        String json = """
                {
                  "codigo_postal": "50000",
                  "estado": "México",
                  "municipio": "Toluca",
                  "ciudad": "Toluca de Lerdo",
                  "zona": "Urbano",
                  "colonias": [
                    {"nombre": "Zopilocalco", "tipo": "Colonia"},
                    {"nombre": "Toluca de Lerdo Centro", "tipo": "Colonia"},
                    {"nombre": "Toluca de Lerdo Centro", "tipo": "Colonia"}
                  ]
                }
                """;
        HttpResponse<String> resp = mockResponse(200, json);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(resp);

        PostaliaCodigoPostalService service = new PostaliaCodigoPostalService(
                httpClient, objectMapper, "https://postalia.com.mx/api/codigos-postales/", "token-prueba");

        Optional<PostaliaCodigoPostalService.CodigoPostalPostalia> primero = service.consultarCodigoPostal(" 50000 ");
        Optional<PostaliaCodigoPostalService.CodigoPostalPostalia> segundo = service.consultarCodigoPostal("50000");

        assertThat(primero).isPresent();
        assertThat(primero.get().codigoPostal()).isEqualTo("50000");
        assertThat(primero.get().estado()).isEqualTo("Estado de México");
        assertThat(primero.get().municipio()).isEqualTo("Toluca");
        assertThat(primero.get().colonias()).containsExactly("Toluca de Lerdo Centro", "Zopilocalco");
        assertThat(segundo).isEqualTo(primero);
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @DisplayName("normalizarEstado: mapea nombres oficiales largos al catálogo local")
    void normalizarEstado_mapeaVariantesOficiales() {
        assertThat(PostaliaCodigoPostalService.normalizarEstado("Coahuila de Zaragoza")).isEqualTo("Coahuila");
        assertThat(PostaliaCodigoPostalService.normalizarEstado("Michoacán de Ocampo")).isEqualTo("Michoacán");
        assertThat(PostaliaCodigoPostalService.normalizarEstado("Veracruz de Ignacio de la Llave")).isEqualTo("Veracruz");
        assertThat(PostaliaCodigoPostalService.normalizarEstado("México")).isEqualTo("Estado de México");
        assertThat(PostaliaCodigoPostalService.normalizarEstado("Ciudad de México")).isEqualTo("Ciudad de México");
    }

    @Test
    @DisplayName("consultarCodigoPostal: cachea 404 sin repetir peticiones")
    void consultarCodigoPostal_cuando404_retornaVacioYCachea() throws Exception {
        HttpResponse<String> resp = mockResponse(404, "");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(resp);

        PostaliaCodigoPostalService service = new PostaliaCodigoPostalService(
                httpClient, objectMapper, "https://postalia.com.mx/api/codigos-postales", "token-prueba");

        assertThat(service.consultarCodigoPostal("99999")).isEmpty();
        assertThat(service.consultarCodigoPostal("99999")).isEmpty();
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @DisplayName("consultarCodigoPostal: CP inválido o token vacío no realiza peticiones HTTP")
    void consultarCodigoPostal_cuandoFormatoInvalidoOTokenVacio_noLlamaHttp() {
        PostaliaCodigoPostalService sinToken = new PostaliaCodigoPostalService(
                httpClient, objectMapper, "https://postalia.com.mx/api/codigos-postales", "");
        PostaliaCodigoPostalService conToken = new PostaliaCodigoPostalService(
                httpClient, objectMapper, "https://postalia.com.mx/api/codigos-postales", "token-prueba");

        assertThat(sinToken.consultarCodigoPostal("01000")).isEmpty();
        assertThat(conToken.consultarCodigoPostal("1234")).isEmpty();
        assertThat(conToken.consultarCodigoPostal(null)).isEmpty();
        verifyNoInteractions(httpClient);
    }

    @Test
    @DisplayName("consultarCodigoPostal: en 500 o IOException no cachea y reintenta")
    void consultarCodigoPostal_cuandoErrorTransitorio_reintenta() throws Exception {
        HttpResponse<String> resp500 = mockResponse(500, "");
        HttpResponse<String> resp200 = mockResponse(200, """
                {"codigo_postal":"62550","estado":"Morelos","municipio":"Jiutepec","colonias":[{"nombre":"Centro Jiutepec"}]}
                """);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(resp500)
                .thenThrow(new IOException("timeout"))
                .thenReturn(resp200);

        PostaliaCodigoPostalService service = new PostaliaCodigoPostalService(
                httpClient, objectMapper, "https://postalia.com.mx/api/codigos-postales", "token-prueba");

        assertThat(service.consultarCodigoPostal("62550")).isEmpty();
        assertThat(service.consultarCodigoPostal("62550")).isEmpty();
        Optional<PostaliaCodigoPostalService.CodigoPostalPostalia> ok = service.consultarCodigoPostal("62550");

        assertThat(ok).isPresent();
        assertThat(ok.get().municipio()).isEqualTo("Jiutepec");
        verify(httpClient, times(3)).send(any(), any());
    }
}
