package com.sah.notificacion;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia los correos cuando ocurre algo en los servicios.
 *
 * - AFTER_COMMIT: el correo solo sale si los datos quedaron guardados;
 *   si la transaccion se revierte, no se avisa de algo que no existe.
 * - @Async: el envio ocurre en otro hilo, asi un servidor SMTP lento no
 *   hace esperar al administrador (rendimiento en la carga masiva).
 *
 * Los servicios no conocen esta clase: solo publican el evento.
 */
@Component
public class NotificacionCorreoListener {

    private final CorreoService correoService;

    public NotificacionCorreoListener(CorreoService correoService) {
        this.correoService = correoService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarUsuario(UsuarioRegistradoEvento evento) {
        correoService.enviarBienvenida(
                evento.correo(), evento.nombres(), evento.claveTemporal());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCrearAsignacion(AsignacionCreadaEvento evento) {
        correoService.enviarAvisoAsignacion(
                evento.correo(), evento.nombres(), evento.tipo(), evento.titulo());
    }
}
