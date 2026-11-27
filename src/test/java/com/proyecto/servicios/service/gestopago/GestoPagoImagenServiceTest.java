package com.proyecto.servicios.service.gestopago;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoImagenServiceTest {

    private static final byte[] PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01};

    @Mock
    private HttpClient httpClient;

    @SuppressWarnings("unchecked")
    private HttpResponse<byte[]> mockResponse(int status, String contentType, byte[] body) {
        HttpResponse<byte[]> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(status);
        when(resp.body()).thenReturn(body);
        if (status == 200) {
            HttpHeaders headers = HttpHeaders.of(
                    contentType == null ? Map.of() : Map.of("Content-Type", List.of(contentType)),
                    (k, v) -> true);
            when(resp.headers()).thenReturn(headers);
        }
        return resp;
    }

    @Test
    @DisplayName("obtenerImagen: retorna PNG sin charset y cachea llamadas posteriores")
    void obtenerImagen_cuando200_retornaYCachea() throws Exception {
        HttpResponse<byte[]> resp = mockResponse(200, "image/png;charset=UTF-8", PNG_BYTES);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(resp);

        GestoPagoImagenService service = new GestoPagoImagenService(
                httpClient, "https://gestopago.portalventas.net/sistema/images/gestopago/servicios/");

        Optional<GestoPagoImagenService.ImagenServicio> primero = service.obtenerImagen(56);
        Optional<GestoPagoImagenService.ImagenServicio> segundo = service.obtenerImagen(56);

        assertThat(primero).isPresent();
        assertThat(primero.get().bytes()).isEqualTo(PNG_BYTES);
        assertThat(primero.get().contentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(segundo).isEqualTo(primero);
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @DisplayName("obtenerImagen: retorna vacío en 404 y cachea resultado negativo")
    void obtenerImagen_cuando404_retornaVacioYCachea() throws Exception {
        HttpResponse<byte[]> resp = mockResponse(404, null, new byte[0]);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(resp);

        GestoPagoImagenService service = new GestoPagoImagenService(
                httpClient, "https://gestopago.portalventas.net/sistema/images/gestopago/servicios");

        assertThat(service.obtenerImagen(3470)).isEmpty();
        assertThat(service.obtenerImagen(3470)).isEmpty();
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @DisplayName("obtenerImagen: retorna vacío sin consultar HTTP para ID nulo o <= 0")
    void obtenerImagen_cuandoIdInvalido_noLlamaHttp() throws Exception {
        GestoPagoImagenService service = new GestoPagoImagenService(
                httpClient, "https://gestopago.portalventas.net/sistema/images/gestopago/servicios");

        assertThat(service.obtenerImagen(null)).isEmpty();
        assertThat(service.obtenerImagen(0)).isEmpty();
        assertThat(service.obtenerImagen(-5)).isEmpty();
        verifyNoInteractions(httpClient);
    }

    @Test
    @DisplayName("obtenerImagen: en 500 o IOException retorna vacío sin cachear y reintenta")
    void obtenerImagen_cuandoErrorTransitorio_noCachea() throws Exception {
        HttpResponse<byte[]> resp500 = mockResponse(500, null, new byte[0]);
        HttpResponse<byte[]> respOk = mockResponse(200, "image/png", PNG_BYTES);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(resp500)
                .thenThrow(new IOException("timeout"))
                .thenReturn(respOk);

        GestoPagoImagenService service = new GestoPagoImagenService(
                httpClient, "https://gestopago.portalventas.net/sistema/images/gestopago/servicios");

        assertThat(service.obtenerImagen(79)).isEmpty();
        assertThat(service.obtenerImagen(79)).isEmpty();
        Optional<GestoPagoImagenService.ImagenServicio> recuperado = service.obtenerImagen(79);

        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().bytes()).isEqualTo(PNG_BYTES);
        verify(httpClient, times(3)).send(any(), any());
    }
}
