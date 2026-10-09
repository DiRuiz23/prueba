package com.proyecto.servicios.repositorys.banco;

import com.proyecto.servicios.entity.banco.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busca un usuario por su correo electrónico (único por regla de negocio). */
    Optional<Usuario> findByCorreo(String correo);

    /** Verifica si existe un usuario con el correo indicado. */
    boolean existsByCorreo(String correo);

    /** Obtiene el usuario asociado a un cliente (relación 1-a-1). */
    Optional<Usuario> findByClienteId(Long clienteId);

    /** Filtra usuarios por estado activo o inactivo. */
    List<Usuario> findByActivo(Boolean activo);

    /** Búsqueda parcial e insensible a mayúsculas por correo. */
    List<Usuario> findByCorreoContainingIgnoreCase(String correo);
}