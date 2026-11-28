package com.sah.dto;

import java.util.List;

/**
 * Resumen de una carga masiva por CSV.
 * Las filas válidas se registran aunque otras tengan errores;
 * cada error indica la línea para que el administrador la corrija.
 */
public record ResultadoCargaDTO(
        int totalFilas,
        int creados,
        int rechazados,
        List<ErrorFilaDTO> errores
) {
}
