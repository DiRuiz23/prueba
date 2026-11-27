package com.proyecto.servicios.repository.banco;

import com.proyecto.servicios.entity.banco.SesionActiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionActivaRepository extends JpaRepository<SesionActiva, Long> {

    Optional<SesionActiva> findByJtiAndActivaTrue(String jti);

    Optional<SesionActiva> findFirstByUsuarioIdAndActivaTrueOrderByFechaCreacionDesc(Long usuarioId);

    List<SesionActiva> findAllByUsuarioIdAndActivaTrue(Long usuarioId);

    @Modifying
    @Query("UPDATE SesionActiva s SET s.activa = false WHERE s.usuario.id = :usuarioId AND s.activa = true")
    int invalidarSesionesActivasPorUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("UPDATE SesionActiva s SET s.activa = false WHERE s.jti = :jti")
    int invalidarSesionPorJti(@Param("jti") String jti);

    @Modifying
    @Query("UPDATE SesionActiva s SET s.fechaUltimoAcceso = :ahora WHERE s.jti = :jti AND s.activa = true")
    int actualizarUltimoAcceso(@Param("jti") String jti, @Param("ahora") OffsetDateTime ahora);
}

