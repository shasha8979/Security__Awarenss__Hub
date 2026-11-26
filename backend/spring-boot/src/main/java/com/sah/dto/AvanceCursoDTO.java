package com.sah.dto;

/**
 * Avance de las asignaciones de un curso.
 */
public record AvanceCursoDTO(
        Integer cursoId,
        String curso,
        String estadoCurso,
        long asignados,
        long pendientes,
        long enProgreso,
        long completados
) {

    public int porcentajeCompletado() {
        return Porcentaje.de(completados, asignados);
    }
}
