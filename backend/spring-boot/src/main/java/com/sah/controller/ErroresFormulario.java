package com.sah.controller;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte los errores de @Valid en un mapa campo -> mensaje para
 * mostrarlos junto a cada campo del formulario (errores['correo']).
 */
final class ErroresFormulario {

    /** Clave para errores que no son de un campo (reglas del servicio). */
    static final String GENERAL = "general";

    private ErroresFormulario() {
    }

    static Map<String, String> de(BindingResult resultado) {
        Map<String, String> errores = new LinkedHashMap<>();

        for (FieldError error : resultado.getFieldErrors()) {
            String mensaje = error.isBindingFailure()
                    ? "El valor ingresado no es válido."
                    : error.getDefaultMessage();
            errores.putIfAbsent(error.getField(), mensaje);
        }
        return errores;
    }

    static Map<String, String> general(String mensaje) {
        return Map.of(GENERAL, mensaje);
    }
}
