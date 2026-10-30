package com.proyecto.servicios.entity.catalogos;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cat_estados")
@Data
public class EstadoCatalogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_id", nullable = false)
    private Pais pais;
}
