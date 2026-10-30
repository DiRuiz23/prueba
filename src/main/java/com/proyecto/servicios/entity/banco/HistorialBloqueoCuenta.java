package com.proyecto.servicios.entity.banco;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_bloqueo_cuenta")
@Data
public class HistorialBloqueoCuenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "estatus_anterior", nullable = false, length = 20)
    private String estatusAnterior;

    @Column(name = "estatus_nuevo", nullable = false, length = 20)
    private String estatusNuevo;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        this.fechaCambio = LocalDateTime.now();
    }
}
