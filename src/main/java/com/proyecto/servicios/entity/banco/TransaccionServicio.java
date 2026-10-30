package com.proyecto.servicios.entity.banco;

import com.proyecto.servicios.enums.EstatusTransaccionServicio;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transacciones_servicios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "hora_local", length = 14)
    private String horaLocal;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(nullable = false, unique = true, length = 50)
    private String upc;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "tipo_front", nullable = false)
    @Builder.Default
    private Integer tipoFront = 1;

    @Column(name = "telefono_referencia", nullable = false, length = 100)
    private String telefonoReferencia;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal comision = BigDecimal.ZERO;

    @Column(name = "monto_debitado", precision = 15, scale = 2)
    private BigDecimal montoDebitado;

    @Column(name = "saldo_anterior", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", precision = 15, scale = 2)
    private BigDecimal saldoPosterior;

    @Column(name = "id_tx_gestopago", length = 60)
    private String idTxGestopago;

    @Column(name = "numero_autorizacion", length = 60)
    private String numeroAutorizacion;

    @Column(name = "codigo_respuesta", length = 20)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", length = 255)
    private String mensajeRespuesta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstatusTransaccionServicio estatus;

    @Column(name = "intentos_confirmacion", nullable = false)
    @Builder.Default
    private Integer intentosConfirmacion = 0;

    @Column(name = "proxima_confirmacion_at")
    private OffsetDateTime proximaConfirmacionAt;

    @Column(name = "fecha_limite_conciliacion")
    private OffsetDateTime fechaLimiteConciliacion;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "fecha_transaccion", nullable = false, updatable = false)
    private OffsetDateTime fechaTransaccion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
