package com.sah;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @EnableAsync activa @Async: los correos se envian en segundo plano
 * (com.sah.notificacion.NotificacionCorreoListener).
 */
@SpringBootApplication
@EnableAsync
public class SecurityAwarenessHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityAwarenessHubApplication.class, args);
    }
}