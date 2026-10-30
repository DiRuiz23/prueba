package com.proyecto.servicios.repository.banco;

import com.proyecto.servicios.entity.banco.TransaccionServicio;
import com.proyecto.servicios.enums.EstatusTransaccionServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionServicioRepository extends JpaRepository<TransaccionServicio, Long> {

    Optional<TransaccionServicio> findByUpc(String upc);

    boolean existsByUpc(String upc);

    List<TransaccionServicio> findByCuentaId(Long cuentaId);

    Optional<TransaccionServicio> findByCuentaIdAndIdempotencyKey(Long cuentaId, String idempotencyKey);

    Optional<TransaccionServicio> findByIdempotencyKey(String idempotencyKey);

    List<TransaccionServicio> findByEstatusAndProximaConfirmacionAtLessThanEqual(
            EstatusTransaccionServicio estatus,
            OffsetDateTime fecha
    );

    @Query(value = "SELECT * FROM transacciones_servicios ts " +
                   "WHERE ts.estatus = 'EN_CONCILIACION' " +
                   "AND (ts.proxima_confirmacion_at IS NULL OR ts.proxima_confirmacion_at <= :ahora) " +
                   "ORDER BY ts.proxima_confirmacion_at ASC NULLS FIRST " +
                   "LIMIT :limite FOR UPDATE SKIP LOCKED",
           nativeQuery = true)
    List<TransaccionServicio> findCandidatasConciliacionParaProcesar(
            @Param("ahora") OffsetDateTime ahora,
            @Param("limite") int limite
    );
}
