package com.sah.dto;

import com.sah.model.Curso;

import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo para activar o desactivar un curso: {"estado": "inactivo"}.
 */
public record EstadoCursoRequestDTO(

        @NotNull(message = "El estado es obligatorio (activo o inactivo)")
        Curso.EstadoCurso estado
) {
}
