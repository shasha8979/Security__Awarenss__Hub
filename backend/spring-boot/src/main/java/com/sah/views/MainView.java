
package com.sah.views;
import com.sah.repository.UsuarioRepository;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;


import jakarta.annotation.security.RolesAllowed;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("")
@PageTitle("Dashboard | Security Awareness Hub")
@RolesAllowed("ROLE_ADMIN")
public class MainView extends AppLayout {
private final UsuarioRepository usuarioRepository;

    
    public MainView(UsuarioRepository usuarioRepository) {

    this.usuarioRepository = usuarioRepository;

    crearEncabezado();
    crearMenuLateral();
    crearContenido();
}
    private void crearEncabezado() {

        DrawerToggle toggle = new DrawerToggle();

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

        Span usuario = new Span(obtenerUsuario());
        usuario.getStyle()
                .set("font-size", "13px")
                .set("font-weight", "600")
                .set("color", "#334155");

        HorizontalLayout encabezado =
                new HorizontalLayout(toggle, identidad, usuario);

        encabezado.setWidthFull();
        encabezado.setAlignItems(
                HorizontalLayout.Alignment.CENTER
        );
        encabezado.expand(identidad);

        encabezado.getStyle()
                .set("padding", "10px 20px")
                .set("background", "#FFFFFF")
                .set("border-bottom", "1px solid #E2E8F0");

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

        menu.add(
                crearOpcion("Inicio", VaadinIcon.DASHBOARD),
                crearOpcion("Usuarios", VaadinIcon.USERS),
                crearOpcion("Empresas", VaadinIcon.BUILDING),
                crearOpcion("Cursos", VaadinIcon.BOOK),
                crearOpcion("Evaluaciones", VaadinIcon.CLIPBOARD_CHECK),
                crearOpcion("Reportes", VaadinIcon.CHART)
        );

        menu.getStyle()
                .set("background", "#FFFFFF");

        addToDrawer(menu);
    }

    private HorizontalLayout crearOpcion(
            String texto,
            VaadinIcon icono) {

        HorizontalLayout opcion = new HorizontalLayout();

        Span iconoVisual = new Span(icono.create());
        iconoVisual.getStyle()
                .set("color", "#2875C7");

        Span etiqueta = new Span(texto);
        etiqueta.getStyle()
                .set("font-size", "14px")
                .set("font-weight", "500")
                .set("color", "#334155");

        opcion.add(iconoVisual, etiqueta);
        opcion.setAlignItems(
                HorizontalLayout.Alignment.CENTER
        );

        opcion.setWidthFull();

        opcion.getStyle()
                .set("padding", "12px")
                .set("border-radius", "8px")
                .set("cursor", "default");

        return opcion;
    }

    private void crearContenido() {

        VerticalLayout contenido = new VerticalLayout();

        contenido.setPadding(true);
        contenido.setSpacing(true);
        contenido.setWidthFull();

        contenido.getStyle()
                .set("background", "#F1F5F9")
                .set("min-height", "100vh")
                .set("box-sizing", "border-box");

        H1 titulo = new H1("Panel administrativo");

        titulo.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700")
                .set("color", "#1E293B")
                .set("margin", "0");

        Paragraph bienvenida = new Paragraph(
                "Bienvenido a Security Awareness Hub. "
                + "Administra la capacitación en seguridad digital."
        );

        bienvenida.getStyle()
                .set("color", "#64748B")
                .set("margin-top", "0");

        HorizontalLayout estadisticas = new HorizontalLayout(
                crearTarjeta("Usuarios",
        String.valueOf(usuarioRepository.count()),
        VaadinIcon.USERS, "#2875C7"),
                crearTarjeta("Empresas", "Por conectar",
                        VaadinIcon.BUILDING, "#0891B2"),

                crearTarjeta("Cursos", "Por conectar",
                        VaadinIcon.BOOK, "#7C3AED"),

                crearTarjeta("Evaluaciones", "Por conectar",
                        VaadinIcon.CLIPBOARD_CHECK, "#D97706")
        );

        estadisticas.setWidthFull();
        estadisticas.setSpacing(true);
        estadisticas.setFlexGrow(1);

        estadisticas.getStyle()
                .set("flex-wrap", "wrap");

        Div actividad = new Div();

        H2 actividadTitulo = new H2("Actividad reciente");

        actividadTitulo.getStyle()
                .set("font-size", "19px")
                .set("color", "#1E293B");

        Paragraph actividadMensaje = new Paragraph(
                "Aquí se mostrarán los registros y movimientos "
                + "recientes de la plataforma."
        );

        actividadMensaje.getStyle()
                .set("color", "#64748B");

        actividad.add(actividadTitulo, actividadMensaje);

        actividad.setWidthFull();

        actividad.getStyle()
                .set("background", "#FFFFFF")
                .set("padding", "22px")
                .set("border-radius", "12px")
                .set("border", "1px solid #E2E8F0")
                .set("box-sizing", "border-box");

        contenido.add(
                titulo,
                bienvenida,
                estadisticas,
                actividad
        );

        setContent(contenido);
    }

    private Div crearTarjeta(
            String titulo,
            String valor,
            VaadinIcon icono,
            String color) {

        Div tarjeta = new Div();

        Span iconoVisual = new Span(icono.create());

        iconoVisual.getStyle()
                .set("color", color)
                .set("font-size", "24px");

        Span nombre = new Span(titulo);

        nombre.getStyle()
                .set("font-size", "14px")
                .set("color", "#64748B");

        H2 cantidad = new H2(valor);

        cantidad.getStyle()
                .set("font-size", "20px")
                .set("color", "#1E293B")
                .set("margin", "8px 0 0 0");

        VerticalLayout informacion =
                new VerticalLayout(nombre, cantidad);

        informacion.setPadding(false);
        informacion.setSpacing(false);

        tarjeta.add(iconoVisual, informacion);

        tarjeta.getStyle()
                .set("background", "#FFFFFF")
                .set("padding", "20px")
                .set("border-radius", "12px")
                .set("border", "1px solid #E2E8F0")
                .set("box-sizing", "border-box")
                .set("flex", "1 1 190px")
                .set("min-width", "190px");

        return tarjeta;
    }

    private String obtenerUsuario() {

        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion != null
                && autenticacion.isAuthenticated()) {

            return autenticacion.getName();
        }

        return "Administrador";
    }
}
