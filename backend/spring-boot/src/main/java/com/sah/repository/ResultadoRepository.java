package com.sah.repository;

import com.sah.model.Resultado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultadoRepository extends JpaRepository<Resultado, Integer> {

    /** Todos los intentos, del más reciente al más antiguo. */
    List<Resultado> findAllByOrderByFechaPresentacionDescIdDesc();

    List<Resultado> findByEvaluacionIdOrderByFechaPresentacionDescIdDesc(Integer evaluacionId);

    List<Resultado> findByUsuarioIdOrderByFechaPresentacionDescIdDesc(Integer usuarioId);

    /** Número del último intento de un usuario en una evaluación (0 si nunca la presentó). */
    @Query("""
            SELECT COALESCE(MAX(r.intento), 0) FROM Resultado r
            WHERE r.usuarioId = :usuarioId AND r.evaluacionId = :evaluacionId
            """)
    int findUltimoIntento(@Param("usuarioId") Integer usuarioId,
                          @Param("evaluacionId") Integer evaluacionId);

    /** Un intento con sus respuestas ya cargadas (para ver el detalle en Vaadin). */
    @Query("""
            SELECT r FROM Resultado r
            LEFT JOIN FETCH r.respuestas
            WHERE r.id = :id
            """)
    Optional<Resultado> findConRespuestasById(@Param("id") Integer id);

    boolean existsByEvaluacionId(Integer evaluacionId);
}
