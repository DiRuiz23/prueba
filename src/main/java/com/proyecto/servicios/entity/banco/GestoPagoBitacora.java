package com.proyecto.servicios.entity.banco;

import com.proyecto.servicios.enums.TipoOperacionBitacora;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "gestopago_bitacora")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GestoPagoBitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_servicio_id")
    private TransaccionServicio transaccionServicio;

    @Column(length = 50)
    private String upc;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacion", nullable = false, length = 30)
    private TipoOperacionBitacora tipoOperacion;

    @Column(name = "request_enmascarado", columnDefinition = "TEXT")
    private String requestEnmascarado;

    @Column(name = "response_enmascarado", columnDefinition = "TEXT")
    private String responseEnmascarado;

    @Column(name = "codigo_http")
    private Integer codigoHttp;

    @Column(name = "codigo_respuesta_proveedor", length = 20)
    private String codigoRespuestaProveedor;

    @Column(name = "duracion_ms")
    private Long duracionMs;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private OffsetDateTime fechaRegistro;
}
