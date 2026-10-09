package com.proyecto.servicios.entity.catalogos;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cat_paises")
@Data
public class Pais {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "codigo_iso", nullable = false, length = 3, unique = true)
    private String codigoIso;
}
