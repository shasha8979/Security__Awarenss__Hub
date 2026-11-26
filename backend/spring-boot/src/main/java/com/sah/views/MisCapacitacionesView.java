
package com.sah.views;

import com.sah.entity.Asignacion;
import com.sah.entity.Usuario;
import com.sah.repository.AsignacionRepository;
import com.sah.repository.UsuarioRepository;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "mis-capacitaciones", layout = MainLayout.class)
@PageTitle("Mis capacitaciones | Security Awareness Hub")
@RolesAllowed("EMPLEADO")
public class MisCapacitacionesView extends VerticalLayout {

    private final AsignacionRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;

    private final Grid<Asignacion> grid =
            new Grid<>(Asignacion.class, false);

    public MisCapacitacionesView(
            AsignacionRepository asignacionRepository,
            UsuarioRepository usuarioRepository) {

        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        construirVista();
    }

    private void construirVista() {

        Usuario estudiante = obtenerEstudianteActual();

        if (estudiante == null) {
            add(
                    new H1("Mis capacitaciones"),
                    new Paragraph(
                            "No fue posible identificar tu cuenta."
                    )
            );
            return;
        }

        add(
                new H1("Mis capacitaciones"),
                new Paragraph(
                        "Aquí puedes consultar los cursos y las evaluaciones "
                                + "asignados a tu cuenta."
                )
        );

        configurarTabla();

        List<Asignacion> asignaciones =
                asignacionRepository.findByUsuario_Id(estudiante.getId());

        if (asignaciones.isEmpty()) {
            add(
                    new Paragraph(
                            "Todavía no tienes capacitaciones asignadas."
                    )
            );
            return;
        }

        add(new H2("Contenido asignado"));

        grid.setItems(asignaciones);
        add(grid);
    }

    private void configurarTabla() {

        grid.removeAllColumns();

        grid.addColumn(this::obtenerTitulo)
                .setHeader("Capacitación")
                .setAutoWidth(true)
                .setFlexGrow(2);

        grid.addColumn(this::obtenerTipo)
                .setHeader("Tipo")
                .setAutoWidth(true);

        grid.addComponentColumn(asignacion ->
                        crearEstado(asignacion.getEstado()))
                .setHeader("Estado")
                .setAutoWidth(true);

        grid.addColumn(asignacion ->
                        asignacion.getFechaAsignacion() == null
                                ? "Sin fecha"
                                : asignacion.getFechaAsignacion().format(
                                        DateTimeFormatter.ofPattern(
                                                "dd/MM/yyyy HH:mm"
                                        )
                                )
                )
                .setHeader("Fecha de asignación")
                .setAutoWidth(true);

        grid.addComponentColumn(asignacion -> {

            Button boton = new Button(
                    "Ver contenido",
                    event -> abrirContenido(asignacion)
            );

            return boton;

        }).setHeader("Acciones").setAutoWidth(true);

        grid.setWidthFull();
    }

    private String obtenerTitulo(Asignacion asignacion) {

        if (asignacion.getTipo() == Asignacion.Tipo.curso
                && asignacion.getCurso() != null) {
            return asignacion.getCurso().getTitulo();
        }

        if (asignacion.getTipo() == Asignacion.Tipo.evaluacion
                && asignacion.getEvaluacion() != null) {
            return asignacion.getEvaluacion().getTitulo();
        }

        return "Contenido no disponible";
    }

    private String obtenerTipo(Asignacion asignacion) {

        if (asignacion.getTipo() == Asignacion.Tipo.curso) {
            return "Curso";
        }

        if (asignacion.getTipo() == Asignacion.Tipo.evaluacion) {
            return "Evaluación";
        }

        return "No definido";
    }

    private Span crearEstado(Asignacion.Estado estado) {

        String texto;
        String color;

        if (estado == null) {
            texto = "Sin estado";
            color = "gray";
        } else {
            switch (estado) {
                case pendiente:
                    texto = "Pendiente";
                    color = "orange";
                    break;

                case progreso:
                    texto = "En progreso";
                    color = "blue";
                    break;

                case completado:
                    texto = "Completada";
                    color = "green";
                    break;

                default:
                    texto = "Sin estado";
                    color = "gray";
                    break;
            }
        }

        Span etiqueta = new Span(texto);

        etiqueta.getStyle()
                .set("color", color)
                .set("font-weight", "600");

        return etiqueta;
    }

    private Usuario obtenerEstudianteActual() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

              return usuarioRepository
                .findByCorreo(authentication.getName())
                .orElse(null);
    }

    private void abrirContenido(Asignacion asignacion) {

        Dialog dialogo = new Dialog();
        dialogo.setWidth("550px");

        VerticalLayout contenido = new VerticalLayout();
        contenido.setPadding(true);
        contenido.setSpacing(true);

        if (asignacion.getTipo() == Asignacion.Tipo.curso) {

            if (asignacion.getCurso() == null) {
                contenido.add(
                        new H2("Curso no disponible"),
                        new Paragraph(
                                "No se encontró la información del curso."
                        )
                );
            } else {
                contenido.add(
                        new H2(asignacion.getCurso().getTitulo()),
                        new Paragraph("Tipo de capacitación: Curso"),
                        new Paragraph(
                                asignacion.getCurso().getDescripcion() == null
                                        ? "No hay descripción disponible."
                                        : asignacion.getCurso().getDescripcion()
                        )
                );
            }

        } else if (asignacion.getTipo()
                == Asignacion.Tipo.evaluacion) {

            if (asignacion.getEvaluacion() == null) {
                contenido.add(
                        new H2("Evaluación no disponible"),
                        new Paragraph(
                                "No se encontró la información de la evaluación."
                        )
                );
            } else {
                contenido.add(
                        new H2(asignacion.getEvaluacion().getTitulo()),
                        new Paragraph("Tipo de capacitación: Evaluación"),
                        new Paragraph(
                                asignacion.getEvaluacion().getDescripcion() == null
                                        ? "No hay descripción disponible."
                                        : asignacion.getEvaluacion().getDescripcion()
                        ),
                        new Paragraph(
                                "Porcentaje mínimo de aprobación: "
                                        + asignacion.getEvaluacion()
                                                .getPorcentajeAprobacion()
                                        + "%"
                        )
                );
            }
        }

        Button cerrar = new Button(
                "Cerrar",
                event -> dialogo.close()
        );

        contenido.add(cerrar);
        dialogo.add(contenido);
        dialogo.open();
    }
}
