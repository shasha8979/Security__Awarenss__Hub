package com.sah.dto;

import com.sah.model.Asignacion;

import jakarta.validation.constraints.NotNull;

/**
 * Nuevo estado de una asignación: "progreso" o "completado".
 * Un valor fuera de la enumeración responde 400 (JSON no válido).
 */
public record AvanceRequestDTO(

        @NotNull(message = "El estado es obligatorio")
        Asignacion.Estado estado
) {
}
