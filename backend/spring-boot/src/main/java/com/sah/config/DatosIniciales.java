package com.sah.config;

import com.sah.model.Usuario;
import com.sah.service.UsuarioService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Datos iniciales (Sesion 6): equivalente de crear_admin.php del PHP.
 *
 * Al arrancar, si no hay ningún administrador activo, crea uno con el
 * correo y la contraseña de las variables ADMIN_CORREO y ADMIN_PASSWORD
 * (archivo .env). Así una base de datos nueva siempre tiene con quién
 * entrar, sin escribir contraseñas en el código.
 *
 * - Si ya existe un administrador activo, no hace nada.
 * - Si faltan las variables, solo avisa en el log.
 * - Usa UsuarioService: mismas validaciones y cifrado BCrypt que el resto.
 * - Un error aquí se registra, pero no detiene la aplicación.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final UsuarioService usuarioService;
    private final String correoAdmin;
    private final String claveAdmin;

    public DatosIniciales(
            UsuarioService usuarioService,
            @Value("${sah.admin.correo:}") String correoAdmin,
            @Value("${sah.admin.password:}") String claveAdmin) {

        this.usuarioService = usuarioService;
        this.correoAdmin = correoAdmin;
        this.claveAdmin = claveAdmin;
    }

    @Override
    public void run(String... args) {

        if (usuarioService.existeAdministradorActivo()) {
            log.info("Datos iniciales: ya existe un administrador activo; no se crea ninguno.");
            return;
        }

        if (correoAdmin.isBlank() || claveAdmin.isBlank()) {
            log.warn("Datos iniciales: no hay ningún administrador activo. "
                    + "Define ADMIN_CORREO y ADMIN_PASSWORD en .env y reinicia para crearlo.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setNombres("Administrador");
        admin.setApellidos("SAH");
        admin.setCorreo(correoAdmin);
        admin.setRol("admin");
        admin.setEstado("activo");

        try {
            usuarioService.registrarUsuario(admin, claveAdmin, null);
            log.info("Datos iniciales: se creó el administrador {}.", correoAdmin);
        } catch (RuntimeException ex) {
            // Por ejemplo, el correo ya existe como usuario inactivo
            log.warn("Datos iniciales: no se pudo crear el administrador {}: {}",
                    correoAdmin, ex.getMessage());
        }
    }
}
