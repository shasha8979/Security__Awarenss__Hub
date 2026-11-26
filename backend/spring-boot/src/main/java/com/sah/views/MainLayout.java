
package com.sah.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.PermitAll;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Plantilla comun: encabezado y menu lateral.
 *
 * El menu muestra solo las opciones a las que el rol del usuario
 * tiene acceso. Esto es usabilidad: la seguridad real la siguen
 * aplicando las anotaciones @RolesAllowed de cada vista.
 */
@PermitAll
public class MainLayout extends AppLayout {

    private static final String ROL_ADMIN = "ROLE_ADMIN";
    private static final String ROL_ADMIN_EMPRESA = "ROLE_ADMIN_EMPRESA";
    private static final String ROL_ESTUDIANTE = "ROLE_EMPLEADO";

    private final AuthenticationContext authenticationContext;

    public MainLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        crearEncabezado();
        crearMenuLateral();
    }

    private void crearEncabezado() {

        DrawerToggle botonMenu = new DrawerToggle();
        botonMenu.setAriaLabel("Mostrar u ocultar el menú");

        H1 logo = new H1("SAH");
        logo.getStyle()
                .set("font-size", "22px")
                .set("font-weight", "800")
                .set("color", "#1769AA")
                .set("margin", "0");

        Span nombre = new Span("SECURITY AWARENESS HUB");
        nombre.getStyle()
                .set("font-size", "13px")
                .set("font-weight", "600")
                .set("letter-spacing", "1px")
                .set("color", "#475569");

        VerticalLayout identidad = new VerticalLayout(logo, nombre);
        identidad.setPadding(false);
        identidad.setSpacing(false);

        Span usuario = new Span(obtenerUsuario() + " · " + nombreRol());
        usuario.getStyle()
                .set("font-size", "13px")
                .set("font-weight", "600")
                .set("color", "#334155");

        Button cerrarSesion = new Button(
                "Cerrar sesión",
                VaadinIcon.SIGN_OUT.create(),
                event -> authenticationContext.logout()
        );
        cerrarSesion.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout encabezado =
                new HorizontalLayout(botonMenu, identidad, usuario, cerrarSesion);

        encabezado.setWidthFull();
        encabezado.setAlignItems(HorizontalLayout.Alignment.CENTER);
        encabezado.expand(identidad);

        encabezado.getStyle()
                .set("padding", "10px 20px")
                .set("background", "#FFFFFF")
                .set("border-bottom", "1px solid #E2E8F0")
                .set("flex-wrap", "wrap");

        addToNavbar(encabezado);
    }

    private void crearMenuLateral() {

        H2 titulo = new H2("Menú principal");
        titulo.getStyle()
                .set("font-size", "15px")
                .set("color", "#64748B")
                .set("margin", "10px 0 20px 10px");

        VerticalLayout menu = new VerticalLayout();
        menu.setPadding(true);
        menu.setSpacing(false);
        menu.setWidthFull();

        menu.add(titulo);
        menu.add(crearOpcion("Inicio", VaadinIcon.DASHBOARD, MainView.class));

        Set<String> roles = obtenerRoles();

        if (roles.contains(ROL_ADMIN)) {
            menu.add(
                    crearOpcion("Usuarios", VaadinIcon.USERS, UsuarioView.class),
                    crearOpcion("Empresas", VaadinIcon.BUILDING, EmpresaView.class),
                    crearOpcion("Cursos", VaadinIcon.BOOK, CursoView.class),
                    crearOpcion("Evaluaciones", VaadinIcon.CLIPBOARD_CHECK, EvaluacionView.class),
                    crearOpcion("Asignaciones", VaadinIcon.TASKS, AsignacionView.class),
                    crearOpcion("Reportes", VaadinIcon.CHART, ReporteView.class)
            );
        }

        if (roles.contains(ROL_ADMIN_EMPRESA)) {
            menu.add(
                    crearOpcion("Seguimiento", VaadinIcon.CHART, AdminEmpresaView.class),
                    crearOpcion("Asignaciones", VaadinIcon.TASKS, AsignacionView.class)
            );
        }

        if (roles.contains(ROL_ESTUDIANTE)) {
            menu.add(
                    crearOpcion("Mis capacitaciones", VaadinIcon.ACADEMY_CAP,
                            MisCapacitacionesView.class)
            );
        }

        menu.getStyle().set("background", "#FFFFFF");

        addToDrawer(menu);
    }

    private RouterLink crearOpcion(
            String texto,
            VaadinIcon icono,
            Class<? extends Component> vista) {

        RouterLink enlace = new RouterLink();
        enlace.setRoute(vista);

        HorizontalLayout contenido = new HorizontalLayout();

        Span iconoVisual = new Span(icono.create());
        iconoVisual.getStyle().set("color", "#2875C7");

        Span etiqueta = new Span(texto);
        etiqueta.getStyle()
                .set("font-size", "14px")
                .set("font-weight", "500")
                .set("color", "#334155");

        contenido.add(iconoVisual, etiqueta);
        contenido.setAlignItems(HorizontalLayout.Alignment.CENTER);
        contenido.setWidthFull();

        contenido.getStyle()
                .set("padding", "12px")
                .set("border-radius", "8px");

        enlace.add(contenido);
        enlace.getStyle().set("text-decoration", "none");

        return enlace;
    }

    private Set<String> obtenerRoles() {

        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion == null) {
            return Set.of();
        }

        return autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private String nombreRol() {

        Set<String> roles = obtenerRoles();

        if (roles.contains(ROL_ADMIN)) {
            return "Administrador";
        }
        if (roles.contains(ROL_ADMIN_EMPRESA)) {
            return "Administrador de empresa";
        }
        if (roles.contains(ROL_ESTUDIANTE)) {
            return "Estudiante";
        }
        return "";
    }

    private String obtenerUsuario() {

        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion != null && autenticacion.isAuthenticated()) {
            return autenticacion.getName();
        }

        return "Usuario";
    }
}
