package com.sah.notificacion;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integracion del envio de correos (Sesion 8).
 * GreenMail levanta un servidor SMTP real en memoria (puerto 3025):
 * el correo viaja por la red local y se verifica lo que llego.
 */
class CorreoServiceTest {

    @RegisterExtension
    static GreenMailExtension servidorSmtp = new GreenMailExtension(ServerSetupTest.SMTP);

    private static JavaMailSenderImpl remitenteSmtp() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("127.0.0.1");
        sender.setPort(ServerSetupTest.SMTP.getPort());
        sender.setDefaultEncoding("UTF-8");
        return sender;
    }

    @Test
    @DisplayName("Bienvenida: llega al destinatario con usuario y contraseña temporal")
    void enviaBienvenida() throws Exception {
        CorreoService servicio = new CorreoService(remitenteSmtp(), true, "no-reply@sah.local");

        assertTrue(servicio.enviarBienvenida("ana@empresa.com", "Ana", "Clave1234567"));

        MimeMessage[] recibidos = servidorSmtp.getReceivedMessages();
        assertEquals(1, recibidos.length);
        assertEquals("Bienvenido a Security Awareness Hub", recibidos[0].getSubject());
        assertEquals("ana@empresa.com", recibidos[0].getAllRecipients()[0].toString());

        String cuerpo = GreenMailUtil.getBody(recibidos[0]);
        assertTrue(cuerpo.contains("Clave1234567"));
        assertTrue(cuerpo.contains("ana@empresa.com"));
    }

    @Test
    @DisplayName("Asignación: el asunto incluye el título del curso")
    void enviaAvisoAsignacion() throws Exception {
        CorreoService servicio = new CorreoService(remitenteSmtp(), true, "no-reply@sah.local");

        assertTrue(servicio.enviarAvisoAsignacion(
                "luis@empresa.com", "Luis", "el curso", "Phishing"));

        MimeMessage[] recibidos = servidorSmtp.getReceivedMessages();
        assertEquals(1, recibidos.length);
        assertEquals("Nueva capacitación asignada: Phishing", recibidos[0].getSubject());
    }

    @Test
    @DisplayName("Deshabilitado (MAIL_ENABLED=false): no envía nada")
    void deshabilitadoNoEnvia() {
        CorreoService servicio = new CorreoService(remitenteSmtp(), false, "no-reply@sah.local");

        assertFalse(servicio.enviarBienvenida("ana@empresa.com", "Ana", "Clave1234567"));
        assertEquals(0, servidorSmtp.getReceivedMessages().length);
    }

    @Test
    @DisplayName("Servidor SMTP caído: devuelve false y no lanza excepción")
    void servidorCaido() {
        JavaMailSenderImpl sinServidor = new JavaMailSenderImpl();
        sinServidor.setHost("127.0.0.1");
        sinServidor.setPort(1); // puerto sin servicio

        CorreoService servicio = new CorreoService(sinServidor, true, "no-reply@sah.local");

        assertFalse(servicio.enviarBienvenida("ana@empresa.com", "Ana", "Clave1234567"));
    }
}
