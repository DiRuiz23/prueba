package com.proyecto.servicios.repositorys.banco;

import com.proyecto.servicios.entity.banco.DatosBiometricos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatosBiometricosRepository extends JpaRepository<DatosBiometricos, Long> {
    Optional<DatosBiometricos> findByClienteId(Long clienteId);
}
