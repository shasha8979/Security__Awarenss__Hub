package com.sah.dto;

/**
 * Calculo comun de porcentajes para los reportes.
 */
final class Porcentaje {

    private Porcentaje() {
    }

    /** Devuelve parte/total en porcentaje redondeado; 0 si total es 0. */
    static int de(long parte, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(parte * 100.0 / total);
    }
}
