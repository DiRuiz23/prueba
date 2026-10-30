package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.DatosBiometricos;

public interface BiometriaService {
    DatosBiometricos procesarYGuardarBiometria(Cliente cliente, String facialBase64);
}
