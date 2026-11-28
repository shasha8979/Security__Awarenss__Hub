package com.sah.dto;

import com.sah.model.Evaluacion;

import java.time.LocalDateTime;

public record EvaluacionResponseDTO(
        Integer id,
        Integer cursoId,
        String curso,
        String titulo,
        String descripcion,
        Integer porcentajeAprobacion,
        Evaluacion.EstadoEvaluacion estado,
        LocalDateTime fechaCreacion
) {

    public static EvaluacionResponseDTO desde(Evaluacion evaluacion, String tituloCurso) {
        return new EvaluacionResponseDTO(
                evaluacion.getId(),
                evaluacion.getCursoId(),
                tituloCurso,
                evaluacion.getTitulo(),
                evaluacion.getDescripcion(),
                evaluacion.getPorcentajeAprobacion(),
                evaluacion.getEstado(),
                evaluacion.getFechaCreacion()
        );
    }
}
