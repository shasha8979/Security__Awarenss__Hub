package com.sah.notificacion;

/**
 * Se publica cuando se asigna un curso o una evaluación a un estudiante.
 *
 * @param tipo texto para el correo: "el curso" o "la evaluación"
 */
public record AsignacionCreadaEvento(
        String correo,
        String nombres,
        String tipo,
        String titulo
) {
}
