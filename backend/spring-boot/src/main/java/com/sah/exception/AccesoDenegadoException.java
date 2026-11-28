package com.sah.exception;

/**
 * El usuario autenticado no puede realizar la operación (403).
 * Se usa cuando la regla depende de los datos (por ejemplo, el rol
 * del usuario frente a la asignación) y no solo de la ruta.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
