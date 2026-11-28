package com.sah.exception;

/**
 * Se intenta registrar un dato que debe ser unico y ya existe
 * (por ejemplo, el titulo de un curso). La API la traduce a HTTP 409.
 */
public class DatoDuplicadoException extends IllegalArgumentException {

    public DatoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
