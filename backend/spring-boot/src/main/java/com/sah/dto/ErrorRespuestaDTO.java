package com.sah.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato unico de error que devuelve la API REST.
 *
 * @param campos errores por campo cuando falla la validacion
 *               (vacio en los demas casos)
 */
public record ErrorRespuestaDTO(
        LocalDateTime fecha,
        int estado,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> campos
) {
}
