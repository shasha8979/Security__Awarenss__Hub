package com.sah.dto;

/**
 * Indicadores generales de la plataforma calculados a partir
 * de los registros reales de la base de datos.
 */
public record ResumenReporteDTO(
        long empresasTotal,
        long empresasActivas,
        long usuariosTotal,
        long administradores,
        long administradoresEmpresa,
        long estudiantes,
        long estudiantesActivos,
        long cursosTotal,
        long cursosActivos,
        long evaluacionesTotal,
        long evaluacionesActivas,
        long asignacionesTotal,
        long asignacionesPendientes,
        long asignacionesEnProgreso,
        long asignacionesCompletadas
) {

    /** Porcentaje de asignaciones completadas (0 si no hay asignaciones). */
    public int porcentajeCompletado() {
        return Porcentaje.de(asignacionesCompletadas, asignacionesTotal);
    }
}
