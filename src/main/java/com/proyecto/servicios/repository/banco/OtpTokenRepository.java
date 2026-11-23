package com.proyecto.servicios.repository.banco;

import com.proyecto.servicios.entity.banco.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findFirstByUsuarioIdAndPropositoAndUsadoFalseAndBloqueadoFalseOrderByFechaCreacionDesc(
            Long usuarioId, String proposito);

    @Modifying
    @Query("UPDATE OtpToken o SET o.usado = true WHERE o.usuario.id = :usuarioId AND o.proposito = :proposito AND o.usado = false")
    int invalidarOtpsPrevios(@Param("usuarioId") Long usuarioId, @Param("proposito") String proposito);
}

