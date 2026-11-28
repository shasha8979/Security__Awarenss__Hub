
package com.sah.repository;

import com.sah.model.Usuario;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

    // Buscar usuario por correo (inicio de sesión)
    Optional<Usuario> findByCorreo(String correo);

    // Datos iniciales: saber si existe un administrador activo
    boolean existsByRolAndEstado(String rol, String estado);

    // Comprobar si un correo ya está registrado
    boolean existsByCorreo(String correo);

    // Comprobar correo duplicado al editar
    boolean existsByCorreoAndIdNot(String correo, Integer id);

    // Correos ya registrados entre los recibidos (carga masiva):
    // una sola consulta en lugar de una por fila
    @Query("select lower(u.correo) from Usuario u where lower(u.correo) in :correos")
    Set<String> buscarCorreosRegistrados(@Param("correos") Collection<String> correos);

    // Consultar usuarios con sus empresas
    @Override
    @EntityGraph(attributePaths = {"empresa"})
    List<Usuario> findAll();

    // Consultar un usuario con su empresa
    @Override
    @EntityGraph(attributePaths = {"empresa"})
    Optional<Usuario> findById(Integer id);

    // Consultar usuarios de una empresa
    List<Usuario> findByEmpresa_Id(Integer empresaId);

    // Consultar usuarios por rol
    List<Usuario> findByRol(String rol);

    // Consultar usuarios por empresa y rol
    List<Usuario> findByEmpresa_IdAndRol(
            Integer empresaId,
            String rol
    );
}
