package com.proyecto.servicios.entity.banco;

import com.proyecto.servicios.enums.EstatusCuenta;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cuentas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "numero_cuenta", nullable = false, unique = true, length = 20, updatable = false)
    private String numeroCuenta;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstatusCuenta estatus = EstatusCuenta.ACTIVA;

    @Column(name = "pin_hash", length = 255)
    private String pinHash;

    @Column(name = "intentos_fallidos_pin", nullable = false)
    @Builder.Default
    private Integer intentosFallidosPin = 0;

    @Column(name = "bloqueo_pin_hasta")
    private OffsetDateTime bloqueoPinHasta;

    public String getPin() {
        return pinHash;
    }

    public void setPin(String pin) {
        this.pinHash = pin;
    }

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
