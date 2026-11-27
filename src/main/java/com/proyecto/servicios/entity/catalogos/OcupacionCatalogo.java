package com.proyecto.servicios.entity.catalogos;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cat_ocupaciones")
@Data
public class OcupacionCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Integer orden;
}
