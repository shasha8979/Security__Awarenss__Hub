package com.sah.repository;

import com.sah.entity.Asignacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsignacionRepository extends JpaRepository<AsignacionView, Integer> {

    @Override
    @EntityGraph(attributePaths = {"usuario", "curso", "evaluacion"})
    List<AsignacionView> findAll();

    @EntityGraph(attributePaths = {"usuario", "curso", "evaluacion"})
    List<AsignacionView> findByUsuario_Id(Integer usuarioId);

    @EntityGraph(attributePaths = {"usuario", "curso", "evaluacion"})
    List<AsignacionView> findByUsuario_Empresa_Id(Integer empresaId);

    boolean existsByUsuario_IdAndCurso_Id(
            Integer usuarioId,
            Integer cursoId
    );

    boolean existsByUsuario_IdAndEvaluacion_Id(
            Integer usuarioId,
            Integer evaluacionId
    );
}