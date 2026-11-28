package com.sah.dto;

/**
 * Avance de capacitacion de un estudiante.
 */
public record AvanceEstudianteDTO(
        Integer usuarioId,
        String nombre,
        String correo,
        String estado,
        long asignaciones,
        long pendientes,
        long enProgreso,
        long completadas
) {

    public int porcentajeCompletado() {
        return Porcentaje.de(completadas, asignaciones);
    }
}
