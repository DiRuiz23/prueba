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

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistorialTransaccionServiceImpl implements HistorialTransaccionService {

    private static final List<EstatusTransaccionServicio> ESTATUS_HISTORIAL = List.of(
            EstatusTransaccionServicio.EXITOSA,
            EstatusTransaccionServicio.FALLIDA,
            EstatusTransaccionServicio.REVERSADA,
            EstatusTransaccionServicio.EN_CONCILIACION
    );

    private final TransaccionServicioRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PagoServicioResponse> obtenerHistorialExitoso(String correoAutenticado, Pageable pageable) {
        log.info("Consultando historial de transacciones para: {}", correoAutenticado);

        String correoNorm = correoAutenticado != null ? correoAutenticado.trim().toLowerCase() : "";
        Usuario usuario = usuarioRepository.findByCorreo(correoAutenticado)
                .or(() -> usuarioRepository.findByCorreoIgnoreCase(correoNorm))
                .orElseThrow(() -> new NoAutenticadoException("No fue posible verificar la identidad del usuario"));

        Page<TransaccionServicio> paginaTx;
        if (usuario.getCliente() != null) {
            paginaTx = transaccionRepository.findByCuentaClienteIdAndEstatusIn(
                    usuario.getCliente().getId(),
                    ESTATUS_HISTORIAL,
                    pageable
            );
        } else {
            paginaTx = transaccionRepository.findByUsuarioIdAndEstatusIn(
                    usuario.getId(),
                    ESTATUS_HISTORIAL,
                    pageable
            );
        }

        return paginaTx.map(this::mapearAResponse);
    }

    private PagoServicioResponse mapearAResponse(TransaccionServicio tx) {
        EstatusTransaccionServicio est = tx.getEstatus();
        String estatusDto;
        if (est == EstatusTransaccionServicio.FALLIDA) {
            estatusDto = "RECHAZADA";
        } else if (est == EstatusTransaccionServicio.EN_CONCILIACION) {
            estatusDto = "EN_PROCESO";
        } else if (est != null) {
            estatusDto = est.name();
        } else {
            estatusDto = "EXITOSA";
        }

        boolean esRechazo = est == EstatusTransaccionServicio.FALLIDA
                || est == EstatusTransaccionServicio.REVERSADA;
        BigDecimal saldoRestante = (esRechazo && tx.getSaldoAnterior() != null)
                ? tx.getSaldoAnterior()
                : tx.getSaldoPosterior();
        String mensaje = tx.getMensajeRespuesta() != null
                ? tx.getMensajeRespuesta()
                : (esRechazo ? "Transacción rechazada" : "Operación realizada con éxito");

        String idTxGestoPago = normalizarCodigoExterno(tx.getIdTxGestopago());
        String numeroAutorizacion = esRechazo ? null : normalizarCodigoExterno(tx.getNumeroAutorizacion());

        return PagoServicioResponse.builder()
                .idTransaccion(tx.getId())
                .upc(tx.getUpc())
                .idTxGestoPago(idTxGestoPago)
                .numeroAutorizacion(numeroAutorizacion)
                .numeroCuenta(tx.getCuenta() != null ? tx.getCuenta().getNumeroCuenta() : null)
                .montoDebitado(tx.getMontoDebitado() != null ? tx.getMontoDebitado() : tx.getMonto())
                .saldoRestante(saldoRestante)
                .telefonoReferencia(tx.getTelefonoReferencia())
                .idProducto(tx.getIdProducto())
                .idServicio(tx.getIdServicio())
                .estatus(estatusDto)
                .mensaje(mensaje)
                .fechaTransaccion(tx.getFechaTransaccion())
                .build();
    }

    private String normalizarCodigoExterno(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return (limpio.isEmpty() || "-1".equals(limpio) || "0".equals(limpio)) ? null : limpio;
    }
}

