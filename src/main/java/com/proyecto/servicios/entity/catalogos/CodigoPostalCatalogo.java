package com.proyecto.servicios.entity.catalogos;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cat_codigos_postales")
@Data
public class CodigoPostalCatalogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String municipio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoCatalogo estado;
}
