package com.sah.dto;

import com.sah.model.Evaluacion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar una evaluacion.
 * El id, el estado y la fecha de creacion los controla el servidor.
 */
public record EvaluacionRequestDTO(

        @NotNull(message = "El curso es obligatorio (cursoId)")
        Integer cursoId,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 5000, message = "La descripción no puede superar 5000 caracteres")
        String descripcion,

        @NotNull(message = "El porcentaje de aprobación es obligatorio")
        @Min(value = 1, message = "El porcentaje mínimo es 1")
        @Max(value = 100, message = "El porcentaje máximo es 100")
        Integer porcentajeAprobacion
) {

    /** Formulario vacío para registrar (página web). */
    public static EvaluacionRequestDTO vacio() {
        return new EvaluacionRequestDTO(null, null, null, 70);
    }

    /** Datos actuales para el formulario de edición. */
    public static EvaluacionRequestDTO desde(Evaluacion evaluacion) {
        return new EvaluacionRequestDTO(evaluacion.getCursoId(), evaluacion.getTitulo(),
                evaluacion.getDescripcion(), evaluacion.getPorcentajeAprobacion());
    }

    public Evaluacion aEntidad() {
        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setCursoId(cursoId);
        evaluacion.setTitulo(titulo);
        evaluacion.setDescripcion(descripcion);
        evaluacion.setPorcentajeAprobacion(porcentajeAprobacion);
        return evaluacion;
    }
}
