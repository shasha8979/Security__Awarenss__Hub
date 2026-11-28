package com.sah.service;

import com.sah.model.Resultado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Regla "cuenta el mejor intento". No requiere base de datos.
 */
class ResultadoServiceTest {

    private static Resultado intento(int usuario, int evaluacion, int numero, String porcentaje) {
        Resultado r = new Resultado();
        r.setUsuarioId(usuario);
        r.setEvaluacionId(evaluacion);
        r.setIntento(numero);
        r.setPorcentaje(new BigDecimal(porcentaje));
        return r;
    }

    @Test
    void dejaSoloElIntentoConMayorPorcentaje() {
        Resultado i1 = intento(1, 10, 1, "40.00");
        Resultado i2 = intento(1, 10, 2, "90.00");
        Resultado i3 = intento(1, 10, 3, "70.00");

        List<Resultado> mejores = ResultadoService.mejoresIntentos(List.of(i3, i2, i1));

        assertEquals(1, mejores.size());
        assertSame(i2, mejores.get(0));
    }

    @Test
    void enEmpateGanaElIntentoMasAntiguo() {
        Resultado i1 = intento(1, 10, 1, "80.00");
        Resultado i2 = intento(1, 10, 2, "80.00");

        List<Resultado> mejores = ResultadoService.mejoresIntentos(List.of(i2, i1));

        assertEquals(1, mejores.size());
        assertSame(i1, mejores.get(0));
    }

    @Test
    void separaPorUsuarioYPorEvaluacion() {
        Resultado a = intento(1, 10, 1, "50.00");
        Resultado b = intento(2, 10, 1, "60.00");
        Resultado c = intento(1, 20, 1, "70.00");
        Resultado d = intento(1, 10, 2, "55.00");

        List<Resultado> mejores = ResultadoService.mejoresIntentos(List.of(a, b, c, d));

        assertEquals(List.of(b, c, d), mejores);
    }

    @Test
    void listaVaciaDevuelveListaVacia() {
        assertEquals(List.of(), ResultadoService.mejoresIntentos(List.of()));
    }
}
