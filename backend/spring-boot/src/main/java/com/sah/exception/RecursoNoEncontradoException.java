package com.sah.exception;

/**
 * El recurso solicitado (curso, usuario, etc.) no existe.
 * La API la traduce a HTTP 404.
 *
 * Extiende IllegalArgumentException para que las vistas Vaadin que ya
 * capturan esa excepcion sigan mostrando el mensaje sin cambios.
 */
public class RecursoNoEncontradoException extends IllegalArgumentException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
