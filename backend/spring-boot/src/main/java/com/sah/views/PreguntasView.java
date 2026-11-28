package com.sah.views;

import com.sah.model.Evaluacion;
import com.sah.model.Opcion;
import com.sah.model.Pregunta;
import com.sah.service.EvaluacionService;
import com.sah.service.EvaluacionService.OpcionDatos;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Administración de las preguntas y opciones de respuesta de una evaluación.
 * Ruta: /evaluaciones/preguntas/{idEvaluacion}
 */
@Route(value = "evaluaciones/preguntas", layout = MainLayout.class)
@PageTitle("Preguntas | Security Awareness Hub")
@RolesAllowed("ADMIN")
public class PreguntasView extends VerticalLayout implements HasUrlParameter<Integer> {

    private final EvaluacionService evaluacionService;

    private Evaluacion evaluacion;
    private Pregunta preguntaActual;

    private final H1 tituloPagina = new H1("Preguntas");
    private final Paragraph resumen = new Paragraph();

    private final H2 tituloEditor = new H2("Nueva pregunta");
    private final TextArea enunciado = new TextArea("Enunciado");
    private final VerticalLayout contenedorOpciones = new VerticalLayout();
    private final List<FilaOpcion> filasOpciones = new ArrayList<>();

    private final Button agregarOpcion = new Button("Agregar opción", VaadinIcon.PLUS.create());
    private final Button guardar = new Button("Guardar pregunta");
    private final Button cancelar = new Button("Cancelar");

    private final Grid<Pregunta> grid = new Grid<>(Pregunta.class, false);

    public PreguntasView(EvaluacionService evaluacionService) {

        this.evaluacionService = evaluacionService;

        crearEditor();
        configurarTabla();

        RouterLink volver = new RouterLink("← Volver a evaluaciones", EvaluacionView.class);

        resumen.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "0");

        VerticalLayout contenido = new VerticalLayout(
                volver,
                tituloPagina,
                resumen,
                crearEditorLayout(),
                new H2("Preguntas registradas"),
                grid
        );

        contenido.setSizeFull();
        contenido.setPadding(true);
        contenido.setSpacing(true);

        setSizeFull();
        setPadding(false);
        add(contenido);
    }

    @Override
    public void setParameter(BeforeEvent event, Integer evaluacionId) {

        try {
            evaluacion = evaluacionService.buscarPorId(evaluacionId);
        } catch (IllegalArgumentException e) {
            mostrarError("La evaluación solicitada no existe.");
            event.forwardTo(EvaluacionView.class);
            return;
        }

        tituloPagina.setText("Preguntas: " + evaluacion.getTitulo());
        limpiarEditor();
        actualizarTabla();
    }

    // ============================================================
    // EDITOR DE PREGUNTA
    // ============================================================

    private void crearEditor() {

        enunciado.setWidthFull();
        enunciado.setRequiredIndicatorVisible(true);
        enunciado.setMaxLength(EvaluacionService.MAX_ENUNCIADO);
        enunciado.setMinHeight("100px");

        contenedorOpciones.setPadding(false);
        contenedorOpciones.setSpacing(true);
        contenedorOpciones.setWidthFull();

        agregarOpcion.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        agregarOpcion.addClickListener(event -> agregarFila("", false));

        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardar.addClickListener(event -> guardarPregunta());

        cancelar.addClickListener(event -> limpiarEditor());
    }

    private VerticalLayout crearEditorLayout() {

        Span instruccion = new Span(
                "Mínimo " + EvaluacionService.MIN_OPCIONES
                        + " opciones. Marca exactamente una como correcta.");
        instruccion.getStyle().set("font-size", "13px").set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout botones = new HorizontalLayout(guardar, cancelar);

        VerticalLayout editor = new VerticalLayout(
                tituloEditor,
                enunciado,
                new Span("Opciones de respuesta"),
                instruccion,
                contenedorOpciones,
                agregarOpcion,
                botones
        );

        editor.setWidthFull();
        editor.setMaxWidth("850px");
        editor.setPadding(true);
        editor.setSpacing(true);
        editor.getStyle()
                .set("background", "var(--sah-superficie)")
                .set("border", "1px solid var(--sah-borde)")
                .set("border-radius", "12px");

        return editor;
    }

    private void agregarFila(String texto, boolean correcta) {

        FilaOpcion fila = new FilaOpcion(texto, correcta);
        filasOpciones.add(fila);
        contenedorOpciones.add(fila);
        actualizarFilas();
    }

    private void quitarFila(FilaOpcion fila) {

        filasOpciones.remove(fila);
        contenedorOpciones.remove(fila);
        actualizarFilas();
    }

    /** Renumera las etiquetas y evita quitar filas por debajo del mínimo. */
    private void actualizarFilas() {

        boolean puedeQuitar = filasOpciones.size() > EvaluacionService.MIN_OPCIONES;

        for (int i = 0; i < filasOpciones.size(); i++) {
            FilaOpcion fila = filasOpciones.get(i);
            fila.texto.setLabel("Opción " + (i + 1));
            fila.quitar.setEnabled(puedeQuitar);
        }
    }

    /** Hace que el checkbox "Correcta" funcione como selección única. */
    private void marcarUnicaCorrecta(FilaOpcion seleccionada) {

        for (FilaOpcion fila : filasOpciones) {
            if (fila != seleccionada && fila.correcta.getValue()) {
                fila.correcta.setValue(false);
            }
        }
    }

    private void guardarPregunta() {

        List<OpcionDatos> opciones = filasOpciones.stream()
                .map(fila -> new OpcionDatos(fila.texto.getValue(), fila.correcta.getValue()))
                .collect(Collectors.toList());

        try {
            boolean nueva = preguntaActual == null;

            evaluacionService.guardarPregunta(
                    evaluacion.getId(),
                    nueva ? null : preguntaActual.getId(),
                    enunciado.getValue(),
                    opciones
            );

            mostrarExito(nueva
                    ? "Pregunta agregada correctamente."
                    : "Pregunta actualizada correctamente.");

            limpiarEditor();
            actualizarTabla();

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void cargarPregunta(Pregunta pregunta) {

        preguntaActual = pregunta;

        tituloEditor.setText("Editar pregunta " + pregunta.getOrden());
        enunciado.setValue(pregunta.getEnunciado());

        filasOpciones.clear();
        contenedorOpciones.removeAll();

        for (Opcion opcion : pregunta.getOpciones()) {
            agregarFila(opcion.getTexto(), opcion.isEsCorrecta());
        }

        while (filasOpciones.size() < EvaluacionService.MIN_OPCIONES) {
            agregarFila("", false);
        }

        guardar.setText("Actualizar pregunta");
        enunciado.focus();
    }

    private void limpiarEditor() {

        preguntaActual = null;

        tituloEditor.setText("Nueva pregunta");
        enunciado.clear();

        filasOpciones.clear();
        contenedorOpciones.removeAll();

        for (int i = 0; i < EvaluacionService.MIN_OPCIONES; i++) {
            agregarFila("", false);
        }

        guardar.setText("Guardar pregunta");
    }

    // ============================================================
    // TABLA DE PREGUNTAS
    // ============================================================

    private void configurarTabla() {

        grid.addColumn(Pregunta::getOrden)
                .setHeader("#")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(Pregunta::getEnunciado)
                .setHeader("Enunciado")
                .setFlexGrow(3);

        grid.addColumn(p -> p.getOpciones().size())
                .setHeader("Opciones")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getOpciones().stream()
                        .filter(Opcion::isEsCorrecta)
                        .map(Opcion::getTexto)
                        .findFirst()
                        .orElse("Sin respuesta correcta"))
                .setHeader("Respuesta correcta")
                .setFlexGrow(2);

        grid.addComponentColumn(this::crearAcciones)
                .setHeader("Acciones")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("Esta evaluación todavía no tiene preguntas.");
    }

    private HorizontalLayout crearAcciones(Pregunta pregunta) {

        Button subir = new Button(VaadinIcon.ARROW_UP.create());
        subir.addThemeVariants(ButtonVariant.LUMO_SMALL);
        subir.setTooltipText("Subir");
        subir.addClickListener(event -> mover(pregunta, -1));

        Button bajar = new Button(VaadinIcon.ARROW_DOWN.create());
        bajar.addThemeVariants(ButtonVariant.LUMO_SMALL);
        bajar.setTooltipText("Bajar");
        bajar.addClickListener(event -> mover(pregunta, 1));

        Button editar = new Button("Editar", VaadinIcon.EDIT.create());
        editar.addThemeVariants(ButtonVariant.LUMO_SMALL);
        editar.addClickListener(event -> cargarPregunta(pregunta));

        Button eliminar = new Button(VaadinIcon.TRASH.create());
        eliminar.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        eliminar.setTooltipText("Eliminar");
        eliminar.addClickListener(event -> confirmarEliminacion(pregunta));

        HorizontalLayout acciones = new HorizontalLayout(subir, bajar, editar, eliminar);
        acciones.setAlignItems(FlexComponent.Alignment.CENTER);
        return acciones;
    }

    private void actualizarTabla() {

        List<Pregunta> preguntas = evaluacionService.listarPreguntas(evaluacion.getId());
        grid.setItems(preguntas);

        evaluacion = evaluacionService.buscarPorId(evaluacion.getId());

        boolean activa = evaluacion.getEstado() == Evaluacion.EstadoEvaluacion.activo;

        resumen.setText(preguntas.size() + " pregunta(s) · Aprobación: "
                + evaluacion.getPorcentajeAprobacion() + " % · Estado: "
                + (activa ? "activa" : "inactiva (actívala desde la lista de evaluaciones)"));
    }

    private void mover(Pregunta pregunta, int direccion) {

        try {
            evaluacionService.moverPregunta(pregunta.getId(), direccion);
            actualizarTabla();
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void confirmarEliminacion(Pregunta pregunta) {

        ConfirmDialog dialogo = new ConfirmDialog();
        dialogo.setHeader("Eliminar pregunta");
        dialogo.setText("¿Seguro que deseas eliminar la pregunta " + pregunta.getOrden()
                + " y todas sus opciones? Esta acción no se puede deshacer.");
        dialogo.setCancelable(true);
        dialogo.setCancelText("Cancelar");
        dialogo.setConfirmText("Eliminar");
        dialogo.setConfirmButtonTheme("error primary");

        dialogo.addConfirmListener(event -> {
            try {
                evaluacionService.eliminarPregunta(pregunta.getId());

                if (preguntaActual != null && preguntaActual.getId().equals(pregunta.getId())) {
                    limpiarEditor();
                }

                mostrarExito("Pregunta eliminada correctamente.");
                actualizarTabla();

            } catch (IllegalArgumentException e) {
                mostrarError(e.getMessage());
            }
        });

        dialogo.open();
    }

    private void mostrarExito(String mensaje) {
        Notification.show(mensaje, 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void mostrarError(String mensaje) {
        Notification.show(mensaje, 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    // ============================================================
    // FILA DE OPCIÓN (texto + correcta + quitar)
    // ============================================================

    private class FilaOpcion extends HorizontalLayout {

        private final TextField texto = new TextField();
        private final Checkbox correcta = new Checkbox("Correcta");
        private final Button quitar = new Button(VaadinIcon.CLOSE_SMALL.create());

        FilaOpcion(String valorTexto, boolean esCorrecta) {

            texto.setValue(valorTexto == null ? "" : valorTexto);
            texto.setMaxLength(EvaluacionService.MAX_TEXTO_OPCION);
            texto.setWidthFull();

            correcta.setValue(esCorrecta);
            correcta.addValueChangeListener(event -> {
                if (Boolean.TRUE.equals(event.getValue())) {
                    marcarUnicaCorrecta(this);
                }
            });

            quitar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            quitar.setTooltipText("Quitar opción");
            quitar.addClickListener(event -> quitarFila(this));

            setWidthFull();
            setAlignItems(Alignment.BASELINE);
            add(texto, correcta, quitar);
            expand(texto);
        }
    }
}
