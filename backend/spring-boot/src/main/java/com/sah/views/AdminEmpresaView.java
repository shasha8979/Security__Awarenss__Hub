
package com.sah.views;

import com.sah.entity.Empresa;
import com.sah.entity.Usuario;
import com.sah.repository.UsuarioRepository;
import com.sah.repository.EmpresaRepository;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "dashboard-empresa", layout = MainLayout.class)
@PageTitle("Dashboard Empresa | Security Awareness Hub")
@RolesAllowed("ADMIN_EMPRESA")
public class AdminEmpresaView extends VerticalLayout {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    public AdminEmpresaView(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository) {

        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        getStyle()
                .set("background", "#F1F5F9")
                .set("box-sizing", "border-box");

        crearContenido();
    }

    private void crearContenido() {

        H1 titulo = new H1("Panel de empresa");
        titulo.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700")
                .set("color", "#1E293B")
                .set("margin", "0");

        Paragraph bienvenida = new Paragraph(
                "Bienvenido a Security Awareness Hub. "
                + "Administra la capacitación de tu empresa."
        );

        bienvenida.getStyle()
                .set("color", "#64748B");

        Usuario usuario = obtenerUsuarioActual();

        Empresa empresa = usuario.getEmpresa();

        if (empresa == null) {
            add(titulo, new Paragraph(
                    "Tu cuenta no tiene una empresa asignada."
            ));
            return;
        }

        H2 nombreEmpresa = new H2(empresa.getNombre());

        nombreEmpresa.getStyle()
                .set("color", "#1769AA")
                .set("font-size", "21px");

        long totalEmpleados =
                usuarioRepository.findByEmpresa_IdAndRol(
                        empresa.getId(), "empleado"
                ).size();

        HorizontalLayout estadisticas = new HorizontalLayout(
                crearTarjeta(
                        "Empleados",
                        String.valueOf(totalEmpleados),
                        VaadinIcon.USERS,
                        "#2875C7"
                ),
                crearTarjeta(
                        "Empresa",
                        empresa.getNombre(),
                        VaadinIcon.BUILDING,
                        "#0891B2"
                )
        );

        estadisticas.setWidthFull();
        estadisticas.setSpacing(true);
        estadisticas.getStyle().set("flex-wrap", "wrap");

        Div actividad = new Div();

        H2 actividadTitulo = new H2("Actividad de capacitación");

        Paragraph actividadMensaje = new Paragraph(
                "Aquí podrás consultar el seguimiento de los empleados, "
                + "sus cursos y los resultados de sus evaluaciones."
        );

        actividad.add(actividadTitulo, actividadMensaje);

        actividad.setWidthFull();

        actividad.getStyle()
                .set("background", "#FFFFFF")
                .set("padding", "22px")
                .set("border-radius", "12px")
                .set("border", "1px solid #E2E8F0")
                .set("box-sizing", "border-box");

        add(
                titulo,
                bienvenida,
                nombreEmpresa,
                estadisticas,
                actividad
        );
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

    private Usuario obtenerUsuarioActual() {

        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        return usuarioRepository.findByCorreo(
                autenticacion.getName()
        ).orElseThrow(() ->
                new IllegalStateException(
                        "No se encontró el usuario autenticado."
                )
        );
    }
}
