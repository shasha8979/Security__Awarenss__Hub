package com.sah.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo comun para activar o desactivar un recurso:
 * {"estado": "activo"} o {"estado": "inactivo"}.
 *
 * Se usa en empresas, usuarios y evaluaciones. (Cursos usa
 * EstadoCursoRequestDTO, con el mismo formato.)
 */
public record EstadoRequestDTO(

        @NotNull(message = "El estado es obligatorio (activo o inactivo)")
        @Pattern(regexp = "activo|inactivo",
                message = "El estado debe ser activo o inactivo")
        String estado
) {
}
