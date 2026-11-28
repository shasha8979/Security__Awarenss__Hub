package com.sah.repository;

import com.sah.model.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreguntaRepository extends JpaRepository<Pregunta, Integer> {

    /** Preguntas de una evaluación con sus opciones ya cargadas (evita LazyInitializationException en Vaadin). */
    @Query("""
            SELECT DISTINCT p FROM Pregunta p
            LEFT JOIN FETCH p.opciones
            WHERE p.evaluacionId = :evaluacionId
            ORDER BY p.orden ASC, p.id ASC
            """)
    List<Pregunta> findConOpcionesByEvaluacionId(@Param("evaluacionId") Integer evaluacionId);

    /** Una pregunta con sus opciones ya cargadas. */
    @Query("""
            SELECT p FROM Pregunta p
            LEFT JOIN FETCH p.opciones
            WHERE p.id = :id
            """)
    Optional<Pregunta> findConOpcionesById(@Param("id") Integer id);

    List<Pregunta> findByEvaluacionIdOrderByOrdenAscIdAsc(Integer evaluacionId);

    long countByEvaluacionId(Integer evaluacionId);

    @Query("SELECT COALESCE(MAX(p.orden), 0) FROM Pregunta p WHERE p.evaluacionId = :evaluacionId")
    int findMaxOrdenByEvaluacionId(@Param("evaluacionId") Integer evaluacionId);

    /** Total de preguntas agrupado por evaluación: cada fila es [evaluacionId, total]. */
    @Query("SELECT p.evaluacionId, COUNT(p) FROM Pregunta p GROUP BY p.evaluacionId")
    List<Object[]> contarPorEvaluacion();
}
