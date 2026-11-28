
package com.sah.repository;

import com.sah.model.Asignacion;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionRepository
        extends JpaRepository<Asignacion, Integer> {

    @Override
    @EntityGraph(attributePaths = {
            "usuario", "curso", "evaluacion"
    })
    List<Asignacion> findAll();

    @Override
    @EntityGraph(attributePaths = {
            "usuario", "curso", "evaluacion"
    })
    Optional<Asignacion> findById(Integer id);

    @EntityGraph(attributePaths = {
            "usuario", "curso", "evaluacion"
    })
    List<Asignacion> findByUsuario_Id(Integer usuarioId);

    @EntityGraph(attributePaths = {
            "usuario", "curso", "evaluacion"
    })
    List<Asignacion> findByUsuario_Empresa_Id(Integer empresaId);

    boolean existsByUsuario_IdAndCurso_Id(
            Integer usuarioId,
            Integer cursoId
    );

    boolean existsByUsuario_IdAndEvaluacion_Id(
            Integer usuarioId,
            Integer evaluacionId
    );
}
