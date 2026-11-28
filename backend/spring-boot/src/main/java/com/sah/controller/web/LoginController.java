package com.sah.controller.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de inicio de sesión y redirección inicial según el rol
 * (como LoginController del ejercicio Gestor de Tareas).
 *
 * El POST a /login y el /logout los procesa Spring Security
 * (formLogin y logout en SecurityConfig).
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Visitante sin sesión: página pública (landing del frontend).
     * Con sesión: cada rol empieza en su propio panel.
     */
    @GetMapping("/")
    public String inicio(Authentication autenticacion) {

        if (autenticacion == null || autenticacion instanceof AnonymousAuthenticationToken) {
            return "redirect:/landing/index.html";
        }
        if (tieneRol(autenticacion, "ROLE_ADMIN")) {
            return "redirect:/panel";
        }
        if (tieneRol(autenticacion, "ROLE_ADMIN_EMPRESA")) {
            return "redirect:/seguimiento";
        }
        return "redirect:/mis-capacitaciones";
    }

    private static boolean tieneRol(Authentication autenticacion, String rol) {
        return autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(rol));
    }
}
