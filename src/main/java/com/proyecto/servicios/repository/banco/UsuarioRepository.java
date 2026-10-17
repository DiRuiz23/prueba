package com.proyecto.servicios.repository.banco;

import com.proyecto.servicios.entity.banco.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    /** Busca un usuario por su correo electrónico (único por regla de negocio). */
    Optional<Usuario> findByCorreo(String correo);

    /** Verifica si existe un usuario con el correo indicado. */
    boolean existsByCorreo(String correo);

    /** Obtiene el usuario asociado a un cliente (relación 1-a-1). */
    Optional<Usuario> findByClienteId(Long clienteId);

    /** Verifica si el cliente ya tiene usuario de acceso. */
    boolean existsByClienteId(Long clienteId);
}