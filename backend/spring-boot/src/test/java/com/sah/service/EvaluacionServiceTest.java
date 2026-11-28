package com.sah.service;

import com.sah.service.EvaluacionService.OpcionDatos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de las reglas de validación de evaluaciones y preguntas.
 * No requieren base de datos ni contexto de Spring.
 */
class EvaluacionServiceTest {

    // ---------- Datos de la evaluación ----------

    @Test
    void evaluacionValidaNoLanzaError() {
        assertDoesNotThrow(() ->
                EvaluacionService.validarDatosEvaluacion(1, "Phishing", "Descripción", 70));
    }

    @Test
    void evaluacionSinCursoEsInvalida() {
        assertThrows(ValidacionException.class, () ->
                EvaluacionService.validarDatosEvaluacion(null, "Phishing", "Descripción", 70));
    }

    @Test
    void evaluacionConTituloVacioEsInvalida() {
        assertThrows(ValidacionException.class, () ->
                EvaluacionService.validarDatosEvaluacion(1, "   ", "Descripción", 70));
    }

    @Test
    void evaluacionConTituloMuyLargoEsInvalida() {
        assertThrows(ValidacionException.class, () ->
                EvaluacionService.validarDatosEvaluacion(1, "a".repeat(151), "Descripción", 70));
    }

    @Test
    void evaluacionSinDescripcionEsInvalida() {
        assertThrows(ValidacionException.class, () ->
                EvaluacionService.validarDatosEvaluacion(1, "Phishing", "", 70));
    }

    @Test
    void porcentajeFueraDeRangoEsInvalido() {
        assertThrows(ValidacionException.class, () ->
                EvaluacionService.validarDatosEvaluacion(1, "Phishing", "Descripción", -1));
        assertThrows(ValidacionException.class, () ->
                EvaluacionService.validarDatosEvaluacion(1, "Phishing", "Descripción", 101));
        assertThrows(ValidacionException.class, () ->
                EvaluacionService.validarDatosEvaluacion(1, "Phishing", "Descripción", null));
        // Regla unificada con la API REST: el minimo es 1
        assertThrows(ValidacionException.class, () ->
                EvaluacionService.validarDatosEvaluacion(1, "Phishing", "Descripción", 0));
    }

    @Test
    void porcentajeEnLimitesEsValido() {
        assertDoesNotThrow(() ->
                EvaluacionService.validarDatosEvaluacion(1, "Phishing", "Descripción", 1));
        assertDoesNotThrow(() ->
                EvaluacionService.validarDatosEvaluacion(1, "Phishing", "Descripción", 100));
    }

    // ---------- Preguntas y opciones ----------

    @Test
    void preguntaValidaNoLanzaError() {
        assertDoesNotThrow(() -> EvaluacionService.validarPregunta(
                "¿Qué es phishing?",
                List.of(new OpcionDatos("Un engaño por correo", true),
                        new OpcionDatos("Un antivirus", false))));
    }

    @Test
    void preguntaSinEnunciadoEsInvalida() {
        assertThrows(ValidacionException.class, () -> EvaluacionService.validarPregunta(
                " ",
                List.of(new OpcionDatos("A", true), new OpcionDatos("B", false))));
    }

    @Test
    void preguntaConMenosDeDosOpcionesEsInvalida() {
        assertThrows(ValidacionException.class, () -> EvaluacionService.validarPregunta(
                "Pregunta", List.of(new OpcionDatos("A", true))));
        assertThrows(ValidacionException.class, () -> EvaluacionService.validarPregunta(
                "Pregunta", List.of()));
    }

    @Test
    void opcionVaciaEsInvalida() {
        assertThrows(ValidacionException.class, () -> EvaluacionService.validarPregunta(
                "Pregunta",
                List.of(new OpcionDatos("A", true), new OpcionDatos("  ", false))));
    }

    @Test
    void opcionMayorA255CaracteresEsInvalida() {
        assertThrows(ValidacionException.class, () -> EvaluacionService.validarPregunta(
                "Pregunta",
                List.of(new OpcionDatos("a".repeat(256), true), new OpcionDatos("B", false))));
    }

    @Test
    void preguntaSinOpcionCorrectaEsInvalida() {
        ValidacionException e = assertThrows(ValidacionException.class, () ->
                EvaluacionService.validarPregunta(
                        "Pregunta",
                        List.of(new OpcionDatos("A", false), new OpcionDatos("B", false))));
        assertTrue(e.getMessage().contains("tiene 0"));
    }

    @Test
    void preguntaConDosOpcionesCorrectasEsInvalida() {
        ValidacionException e = assertThrows(ValidacionException.class, () ->
                EvaluacionService.validarPregunta(
                        "Pregunta",
                        List.of(new OpcionDatos("A", true),
                                new OpcionDatos("B", true),
                                new OpcionDatos("C", false))));
        assertTrue(e.getMessage().contains("tiene 2"));
    }
}
