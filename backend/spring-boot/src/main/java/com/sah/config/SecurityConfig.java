package com.sah.config;

import com.sah.service.UsuarioDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UsuarioDetailsService usuarioDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(usuarioDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    /**
     * Seguridad de la API REST (/api/**), separada de la interfaz Vaadin.
     *
     * - HTTP Basic: el cliente (Postman, Thunder Client) envia usuario y
     *   contrasena en cada peticion; debe usarse sobre HTTPS en produccion.
     * - Sin sesion (STATELESS): no se crean cookies de sesion.
     * - CSRF desactivado: CSRF ataca sesiones basadas en cookies, y esta
     *   API no las usa. La interfaz Vaadin mantiene su proteccion CSRF.
     * - 401 si no hay credenciales validas, 403 si el rol no alcanza;
     *   ambos en JSON.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider)
            throws Exception {

        http
                .securityMatcher("/api/**")
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(reglas -> reglas
                        // Consultar el catalogo de capacitacion: cualquier
                        // usuario autenticado (admin, admin_empresa, estudiante)
                        .requestMatchers(HttpMethod.GET,
                                "/api/cursos/**", "/api/evaluaciones/**").authenticated()
                        // Asignaciones: cada rol ve las suyas y el estudiante
                        // registra su avance; AsignacionService filtra por rol
                        .requestMatchers(HttpMethod.GET, "/api/asignaciones").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/asignaciones/*/avance")
                                .hasRole("EMPLEADO")
                        // Crear, editar o desactivar cursos y evaluaciones, y
                        // todo lo de empresas y usuarios: solo administrador
                        .anyRequest().hasRole("ADMIN")
                )
                .httpBasic(basic -> basic.authenticationEntryPoint(
                        (peticion, respuesta, ex) -> escribirError(
                                peticion, respuesta, 401, "Unauthorized",
                                "Debes autenticarte con un usuario válido.")
                ))
                .exceptionHandling(errores -> errores.accessDeniedHandler(
                        (peticion, respuesta, ex) -> escribirError(
                                peticion, respuesta, 403, "Forbidden",
                                "Tu rol no tiene permiso para esta operación.")
                ))
                .sessionManagement(sesion ->
                        sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }

    /**
     * Seguridad de las páginas web (Thymeleaf), como en el ejercicio
     * Gestor de Tareas: formulario de login, sesión por cookie y CSRF
     * activado (th:action agrega el token a cada formulario).
     *
     * Las reglas se aplican en el servidor: aunque alguien escriba la URL
     * a mano, un rol sin permiso recibe 403. El orden importa: las reglas
     * específicas van antes de anyRequest().
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider)
            throws Exception {

        http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(reglas -> reglas
                        // Login y estilos (CSS del frontend): públicos
                        .requestMatchers("/login", "/admin/css/**", "/auth/**",
                                "/error").permitAll()
                        // Administración del sistema: solo administrador
                        .requestMatchers("/panel/**", "/usuarios/**", "/empresas/**", "/cursos/**",
                                "/evaluaciones/**", "/reportes/**").hasRole("ADMIN")
                        // Asignaciones: administrador y administrador de empresa
                        .requestMatchers("/asignaciones/**")
                                .hasAnyRole("ADMIN", "ADMIN_EMPRESA")
                        .requestMatchers("/seguimiento/**").hasRole("ADMIN_EMPRESA")
                        .requestMatchers("/mis-capacitaciones/**").hasRole("EMPLEADO")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());

        return http.build();
    }

    private static void escribirError(
            HttpServletRequest peticion,
            HttpServletResponse respuesta,
            int estado,
            String error,
            String mensaje) throws IOException {

        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");

        // Mismo formato que ErrorRespuestaDTO
        respuesta.getWriter().write(
                "{\"fecha\":\"" + LocalDateTime.now() + "\","
                        + "\"estado\":" + estado + ","
                        + "\"error\":\"" + error + "\","
                        + "\"mensaje\":\"" + mensaje + "\","
                        + "\"ruta\":\"" + json(peticion.getRequestURI()) + "\","
                        + "\"campos\":{}}"
        );
    }

    private static String json(String texto) {
        return texto == null ? "" : texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
