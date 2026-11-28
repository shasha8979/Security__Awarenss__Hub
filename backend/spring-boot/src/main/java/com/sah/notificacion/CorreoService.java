package com.sah.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de correos de la plataforma (Sesion 8).
 *
 * - La configuracion SMTP (servidor, usuario, clave) llega por variables
 *   de entorno; nunca se escribe en el codigo.
 * - Con MAIL_ENABLED=false (valor por defecto en desarrollo) no se envia
 *   nada: solo se registra en el log que el correo se omitio.
 * - Un fallo del servidor de correo no detiene la operacion principal
 *   (registro o asignacion): se registra y se devuelve false.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final JavaMailSender mailSender;
    private final boolean habilitado;
    private final String remitente;

    public CorreoService(
            JavaMailSender mailSender,
            @Value("${sah.correo.habilitado:false}") boolean habilitado,
            @Value("${sah.correo.remitente:no-reply@sah.local}") String remitente) {

        this.mailSender = mailSender;
        this.habilitado = habilitado;
        this.remitente = remitente;
    }

    /** Correo de bienvenida con la contraseña temporal del usuario. */
    public boolean enviarBienvenida(String correo, String nombres, String claveTemporal) {

        String texto = "Hola " + nombres + ",\n\n"
                + "Se creó tu cuenta en Security Awareness Hub.\n\n"
                + "Usuario: " + correo + "\n"
                + "Contraseña temporal: " + claveTemporal + "\n\n"
                + "Por seguridad, no compartas esta contraseña con nadie.\n"
                + "Si no esperabas este mensaje, comunícate con el administrador.\n\n"
                + "Security Awareness Hub — Capacita · Concientiza · Protege";

        return enviar(correo, "Bienvenido a Security Awareness Hub", texto);
    }

    /** Aviso al estudiante de que tiene un curso o evaluación nuevo. */
    public boolean enviarAvisoAsignacion(
            String correo, String nombres, String tipo, String titulo) {

        String texto = "Hola " + nombres + ",\n\n"
                + "Se te asignó " + tipo + ": \"" + titulo + "\".\n\n"
                + "Ingresa a Security Awareness Hub, sección \"Mis capacitaciones\", "
                + "para comenzar.\n\n"
                + "Security Awareness Hub — Capacita · Concientiza · Protege";

        return enviar(correo, "Nueva capacitación asignada: " + titulo, texto);
    }

    boolean enviar(String destinatario, String asunto, String texto) {

        if (!habilitado) {
            log.info("Correo deshabilitado (MAIL_ENABLED=false): no se envió \"{}\" a {}",
                    asunto, destinatario);
            return false;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);

        try {
            mailSender.send(mensaje);
            log.info("Correo \"{}\" enviado a {}", asunto, destinatario);
            return true;
        } catch (MailException ex) {
            // No se registra el cuerpo: puede contener la contraseña temporal
            log.warn("No se pudo enviar el correo \"{}\" a {}: {}",
                    asunto, destinatario, ex.getMessage());
            return false;
        }
    }
}
