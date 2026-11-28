
package com.sah.views;

import com.sah.model.Asignacion;
import com.sah.service.AsignacionService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "mis-capacitaciones", layout = MainLayout.class)
@PageTitle("Mis capacitaciones | Security Awareness Hub")
@RolesAllowed("EMPLEADO")
public class MisCapacitacionesView extends VerticalLayout {

    // La vista usa el servicio (no los repositorios): las reglas de quién
    // ve qué y cómo avanza una asignación viven en AsignacionService.
    private final AsignacionService asignacionService;

    private final Grid<Asignacion> grid =
            new Grid<>(Asignacion.class, false);

    private final Paragraph sinAsignaciones = new Paragraph(
            "Todavía no tienes capacitaciones asignadas."
    );

    private final H2 tituloTabla = new H2("Contenido asignado");

    public MisCapacitacionesView(AsignacionService asignacionService) {

        this.asignacionService = asignacionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        construirVista();
    }

    private void construirVista() {

        add(
                new H1("Mis capacitaciones"),
                new Paragraph(
                        "Aquí puedes consultar los cursos y las evaluaciones "
                                + "asignados a tu cuenta. Marca \"Iniciar\" cuando "
                                + "comiences y \"Marcar como completada\" al terminar: "
                                + "tu empresa verá tu avance en sus reportes."
                )
        );

        configurarTabla();

        add(sinAsignaciones, tituloTabla, grid);

        actualizarTabla();
    }

    private void actualizarTabla() {

        List<Asignacion> asignaciones = asignacionService.listarAsignaciones();

        boolean vacia = asignaciones.isEmpty();
        sinAsignaciones.setVisible(vacia);
        tituloTabla.setVisible(!vacia);
        grid.setVisible(!vacia);

        grid.setItems(asignaciones);
    }

    private void registrarAvance(Asignacion asignacion, Asignacion.Estado nuevoEstado) {

        try {
            asignacionService.registrarAvance(asignacion.getId(), nuevoEstado);

            Notification aviso = Notification.show(
                    nuevoEstado == Asignacion.Estado.completado
                            ? "¡Capacitación completada!"
                            : "Capacitación iniciada.");
            aviso.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (RuntimeException ex) {
            Notification aviso = Notification.show(
                    ex.getMessage(), 5000, Notification.Position.MIDDLE);
            aviso.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

        actualizarTabla();
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

            Button ver = new Button(
                    "Ver contenido",
                    event -> abrirContenido(asignacion)
            );

            HorizontalLayout acciones = new HorizontalLayout(ver);

            if (asignacion.getEstado() == Asignacion.Estado.pendiente) {
                Button iniciar = new Button("Iniciar", event ->
                        registrarAvance(asignacion, Asignacion.Estado.progreso));
                iniciar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                acciones.add(iniciar);
            }

            if (asignacion.getEstado() == Asignacion.Estado.progreso) {
                Button completar = new Button("Marcar como completada", event ->
                        registrarAvance(asignacion, Asignacion.Estado.completado));
                completar.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                acciones.add(completar);
            }

            return acciones;

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
            color = "var(--lumo-tertiary-text-color)";
        } else {
            switch (estado) {
                case pendiente:
                    texto = "Pendiente";
                    color = "var(--sah-aviso-texto)";
                    break;

                case progreso:
                    texto = "En progreso";
                    color = "var(--sah-info)";
                    break;

                case completado:
                    texto = "Completada";
                    color = "var(--sah-verde)";
                    break;

                default:
                    texto = "Sin estado";
                    color = "var(--lumo-tertiary-text-color)";
                    break;
            }
        }

        Span etiqueta = new Span(texto);

        etiqueta.getStyle()
                .set("color", color)
                .set("font-weight", "600");

        return etiqueta;
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
