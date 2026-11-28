package com.sah.dto;

/**
 * Fila del CSV que no se pudo registrar.
 *
 * @param fila número de línea en el archivo (la cabecera es la línea 1)
 */
public record ErrorFilaDTO(
        int fila,
        String correo,
        String mensaje
) {
}
