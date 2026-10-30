package com.proyecto.servicios.entity.banco;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "datos_biometricos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosBiometricos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "foto_facial", nullable = false, columnDefinition = "TEXT")
    private String fotoFacial;

    @Column(name = "facial_hash", nullable = false, length = 64)
    private String facialHash;

    @Column(name = "facial_features", columnDefinition = "TEXT")
    private String facialFeatures;

    @Column(name = "proveedor_reconocimiento", length = 50)
    private String proveedorReconocimiento;

    @Column(name = "confianza_coincidencia", precision = 5, scale = 2)
    private BigDecimal confianzaCoincidencia;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_captura", nullable = false, updatable = false)
    private OffsetDateTime fechaCaptura;
}
