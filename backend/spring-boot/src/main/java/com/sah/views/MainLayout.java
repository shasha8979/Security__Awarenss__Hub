
package com.sah.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class MainLayout extends AppLayout {

    public MainLayout() {
        crearEncabezado();
        crearMenuLateral();
    }

    private void crearEncabezado() {

      

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
                new HorizontalLayout( identidad, usuario);

        encabezado.setWidthFull();
        encabezado.setAlignItems(HorizontalLayout.Alignment.CENTER);
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
                crearOpcion("Inicio", VaadinIcon.DASHBOARD, ""),
                crearOpcion("Usuarios", VaadinIcon.USERS, "usuarios"),
                crearOpcion("Empresas", VaadinIcon.BUILDING, "empresas"),
                crearOpcion("Cursos", VaadinIcon.BOOK, "cursos"),
                crearOpcion("Evaluaciones", VaadinIcon.CLIPBOARD_CHECK, "evaluaciones"),
                crearOpcion("Reportes", VaadinIcon.CHART, "reportes")
        );

        menu.getStyle().set("background", "#FFFFFF");

        addToDrawer(menu);
    }

    private RouterLink crearOpcion(
            String texto,
            VaadinIcon icono,
            String ruta) {

        RouterLink enlace = new RouterLink();
        enlace.setRoute(ruta.isEmpty()
                ? MainView.class
                : obtenerVista(ruta));

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

    private Class<? extends com.vaadin.flow.component.Component>
            obtenerVista(String ruta) {

        switch (ruta) {
            case "evaluaciones":
                return EvaluacionView.class;
            default:
                return MainView.class;
        }
    }

    private String obtenerUsuario() {

        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion != null && autenticacion.isAuthenticated()) {
            return autenticacion.getName();
        }

        return "Administrador";
    }
}
