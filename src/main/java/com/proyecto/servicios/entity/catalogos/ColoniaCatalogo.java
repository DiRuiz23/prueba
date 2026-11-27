package com.proyecto.servicios.entity.catalogos;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cat_colonias")
@Data
public class ColoniaCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo_postal", nullable = false, length = 10)
    private String codigoPostal;

    @Column(nullable = false, length = 100)
    private String nombre;
}
