package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.DatosBiometricos;
import com.proyecto.servicios.exception.banco.ReglaNegocioException;
import com.proyecto.servicios.repositorys.banco.DatosBiometricosRepository;
import com.proyecto.servicios.service.banco.BiometriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiometriaServiceImpl implements BiometriaService {

    private final DatosBiometricosRepository datosBiometricosRepository;

    public DatosBiometricos procesarYGuardarBiometria(Cliente cliente, String facialBase64) {
        if (facialBase64 == null || facialBase64.trim().isEmpty()) {
            throw new ReglaNegocioException("La fotografía facial biométrica es mandatoria.");
        }

        // Limpiar encabezados Data-URI si vienen incluidos
        String base64Limpio = facialBase64.contains(",") ? facialBase64.split(",")[1] : facialBase64;

        try {
            byte[] bytesFoto = Base64.getDecoder().decode(base64Limpio);
            if (bytesFoto.length < 5000) { // Menor a ~5KB es una imagen corrupta o inválida
                throw new ReglaNegocioException("Resolución o calidad biométrica insuficiente.");
            }

            // Calcular SHA-256 para integridad criptográfica
            String sha256 = DigestUtils.sha256Hex(bytesFoto);

            // Simulación de cotejo con servicio biométrico externo (AWS Rekognition /
            // Face++)
            // Retorna un score de liveness / confianza
            BigDecimal scoreConfianza = new BigDecimal("99.45");

            DatosBiometricos bio = DatosBiometricos.builder()
                    .cliente(cliente)
                    .fotoFacial(base64Limpio)
                    .facialHash(sha256)
                    .facialFeatures("{\"liveness\": true, \"faceCount\": 1, \"quality\": \"HIGH\"}")
                    .proveedorReconocimiento("INTERNAL_BIOMETRIC_ENGINE")
                    .confianzaCoincidencia(scoreConfianza)
                    .activo(true)
                    .build();

            return datosBiometricosRepository.save(bio);

        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("La cadena de datos biométricos no posee un formato Base64 válido.");
        }
    }
}
