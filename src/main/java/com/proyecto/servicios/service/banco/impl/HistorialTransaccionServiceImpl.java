package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.dto.response.banco.PagoServicioResponse;
import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.enums.EstatusTransaccionServicio;
import com.proyecto.servicios.exception.NoAutenticadoException;
import com.proyecto.servicios.repository.banco.TransaccionServicioRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.service.banco.HistorialTransaccionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistorialTransaccionServiceImpl implements HistorialTransaccionService {

    private final TransaccionServicioRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PagoServicioResponse> obtenerHistorialExitoso(String correoAutenticado, Pageable pageable) {
        log.info("Consultando historial de compras exitosas para: {}", correoAutenticado);

        Usuario usuario = usuarioRepository.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new NoAutenticadoException("No fue posible verificar la identidad del usuario"));

        Page<TransaccionServicio> paginaTx;
        if (usuario.getCliente() != null) {
            paginaTx = transaccionRepository.findByCuentaClienteIdAndEstatus(
                    usuario.getCliente().getId(),
                    EstatusTransaccionServicio.EXITOSA,
                    pageable
            );
        } else {
            paginaTx = transaccionRepository.findByUsuarioIdAndEstatus(
                    usuario.getId(),
                    EstatusTransaccionServicio.EXITOSA,
                    pageable
            );
        }

        return paginaTx.map(this::mapearAResponse);
    }

    private PagoServicioResponse mapearAResponse(TransaccionServicio tx) {
        return PagoServicioResponse.builder()
                .idTransaccion(tx.getId())
                .upc(tx.getUpc())
                .idTxGestoPago(tx.getIdTxGestopago())
                .numeroAutorizacion(tx.getNumeroAutorizacion())
                .numeroCuenta(tx.getCuenta() != null ? tx.getCuenta().getNumeroCuenta() : null)
                .montoDebitado(tx.getMontoDebitado() != null ? tx.getMontoDebitado() : tx.getMonto())
                .saldoRestante(tx.getSaldoPosterior())
                .telefonoReferencia(tx.getTelefonoReferencia())
                .idProducto(tx.getIdProducto())
                .idServicio(tx.getIdServicio())
                .estatus(tx.getEstatus() != null ? tx.getEstatus().name() : "EXITOSA")
                .mensaje(tx.getMensajeRespuesta() != null ? tx.getMensajeRespuesta() : "Operación realizada con éxito")
                .fechaTransaccion(tx.getFechaTransaccion())
                .build();
    }
}

