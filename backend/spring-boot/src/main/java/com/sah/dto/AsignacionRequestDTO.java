package com.sah.dto;

import com.sah.model.Asignacion;

import jakarta.validation.constraints.NotNull;

/**
 * Datos del formulario de asignación (página web).
 * Según el tipo, se usa cursoId o evaluacionId; AsignacionService
 * valida la combinación, el estado del contenido y los permisos del rol.
 */
public record AsignacionRequestDTO(

        @NotNull(message = "Selecciona el estudiante")
        Integer usuarioId,

        @NotNull(message = "Selecciona el tipo de asignación")
        Asignacion.Tipo tipo,

        Integer cursoId,

        Integer evaluacionId
) {

    /** Solo se envía el contenido que corresponde al tipo elegido. */
    public Integer cursoSegunTipo() {
        return tipo == Asignacion.Tipo.curso ? cursoId : null;
    }

    public Integer evaluacionSegunTipo() {
        return tipo == Asignacion.Tipo.evaluacion ? evaluacionId : null;
    }
}
