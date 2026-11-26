package com.sah.dto;

import com.sah.model.Curso;

import java.time.LocalDateTime;

/**
 * Representacion JSON de un curso que devuelve la API.
 * Se usa un DTO para no exponer la entidad JPA directamente.
 */
public record CursoResponseDTO(
        Integer id,
        String titulo,
        String descripcion,
        Curso.NivelDificultad nivelDificultad,
        Integer duracion,
        Integer numeroModulos,
        Integer porcentajeAprobacion,
        Curso.EstadoCurso estado,
        LocalDateTime fechaCreacion
) {

    public static CursoResponseDTO desde(Curso curso) {
        return new CursoResponseDTO(
                curso.getId(),
                curso.getTitulo(),
                curso.getDescripcion(),
                curso.getNivelDificultad(),
                curso.getDuracion(),
                curso.getNumeroModulos(),
                curso.getPorcentajeAprobacion(),
                curso.getEstado(),
                curso.getFechaCreacion()
        );
    }
}
