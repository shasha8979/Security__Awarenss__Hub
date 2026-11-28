package com.sah.dto;

import com.sah.model.Asignacion;

import java.time.LocalDateTime;

/** Asignación devuelta por la API (sin datos sensibles del estudiante). */
public record AsignacionResponseDTO(
        Integer id,
        Asignacion.Tipo tipo,
        Integer contenidoId,
        String titulo,
        Integer estudianteId,
        String estudiante,
        Asignacion.Estado estado,
        LocalDateTime fechaAsignacion
) {

    public static AsignacionResponseDTO desde(Asignacion asignacion) {

        boolean esCurso = asignacion.getTipo() == Asignacion.Tipo.curso;

        return new AsignacionResponseDTO(
                asignacion.getId(),
                asignacion.getTipo(),
                esCurso ? asignacion.getCurso().getId() : asignacion.getEvaluacion().getId(),
                esCurso ? asignacion.getCurso().getTitulo() : asignacion.getEvaluacion().getTitulo(),
                asignacion.getUsuario().getId(),
                asignacion.getUsuario().getNombres() + " " + asignacion.getUsuario().getApellidos(),
                asignacion.getEstado(),
                asignacion.getFechaAsignacion()
        );
    }
}
