
package com.sah.repository;

import com.sah.model.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Integer> {

    List<Evaluacion> findByCursoIdOrderByIdAsc(Integer cursoId);

    // Evitan dos evaluaciones con el mismo titulo dentro de un curso
    boolean existsByCursoIdAndTituloIgnoreCase(Integer cursoId, String titulo);

    boolean existsByCursoIdAndTituloIgnoreCaseAndIdNot(
            Integer cursoId, String titulo, Integer id);
}
