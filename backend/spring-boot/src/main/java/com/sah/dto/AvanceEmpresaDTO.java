package com.sah.dto;

/**
 * Avance de capacitacion de los estudiantes de una empresa.
 */
public record AvanceEmpresaDTO(
        Integer empresaId,
        String empresa,
        long estudiantes,
        long asignaciones,
        long pendientes,
        long enProgreso,
        long completadas
) {

    public int porcentajeCompletado() {
        return Porcentaje.de(completadas, asignaciones);
    }
}
