package com.sah.service;

/**
 * Error de validación de reglas de negocio de evaluaciones, preguntas
 * y resultados. El mensaje está pensado para mostrarse al usuario.
 *
 * Extiende IllegalArgumentException, igual que DatoDuplicadoException y
 * RecursoNoEncontradoException: la API REST la traduce a HTTP 400
 * (ManejadorGlobalErrores) y las vistas Vaadin la capturan como
 * IllegalArgumentException junto con las demás.
 */
public class ValidacionException extends IllegalArgumentException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
