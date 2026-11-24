package com.sah;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class VerificacionHashTest {

    @Test
    void verificarContrasenaReal() {

        String contrasenaReal = "AQUI_VA_LA_CONTRASENA_REAL";

        System.out.println("LONGITUD_CONTRASENA=" + contrasenaReal.length());
        System.out.println("PRIMER_CARACTER_CODIGO=" + (int) contrasenaReal.charAt(0));
        System.out.println("ULTIMO_CARACTER_CODIGO=" + (int) contrasenaReal.charAt(contrasenaReal.length() - 1));
    }
}
