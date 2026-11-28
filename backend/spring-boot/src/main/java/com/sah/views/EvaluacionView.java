package com.sah.views;

import com.sah.model.Curso;
import com.sah.model.Evaluacion;
import com.sah.repository.CursoRepository;
import com.sah.service.EvaluacionService;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestion de evaluaciones (administrador del sistema).
 * Las preguntas y opciones se administran en PreguntasView.
 */
@Route(value = "evaluaciones", layout = MainLayout.class)
@PageTitle("Evaluaciones | Security Awareness Hub")
@RolesAllowed("ADMIN")
public class EvaluacionView extends VerticalLayout {

    private final EvaluacionService evaluacionService;
    private final CursoRepository cursoRepository;

    private final Grid<Evaluacion> grid = new Grid<>(Evaluacion.class, false);
    private final TextField filtro = new TextField();

    private final TextField titulo = new TextField("Título");
    private final TextArea descripcion = new TextArea("Descripción");
    private final ComboBox<Curso> curso = new ComboBox<>("Curso asociado");
    private final IntegerField porcentajeAprobacion =
            new IntegerField("Porcentaje de aprobación");

    private final Button guardar = new Button("Guardar evaluación");
    private final Button cancelar = new Button("Cancelar");

    private Evaluacion evaluacionActual;

    private List<Evaluacion> evaluaciones = List.of();
    private Map<Integer, Curso> cursosPorId = Map.of();
    private Map<Integer, Long> preguntasPorEvaluacion = Map.of();

    public EvaluacionView(
            EvaluacionService evaluacionService,
            CursoRepository cursoRepository) {

        this.evaluacionService = evaluacionService;
        this.cursoRepository = cursoRepository;

        crearFormulario();
        configurarTabla();
        actualizarTabla();

        Paragraph ayuda = new Paragraph(
                "Las evaluaciones nuevas se crean inactivas. Agrega sus preguntas "
                        + "y luego actívalas para poder asignarlas y presentarlas. "
                        + "Las evaluaciones no se eliminan: se desactivan.");
        ayuda.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "0");

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // El menu y el encabezado los aporta MainLayout (layout de la ruta)
        add(
                new H1("Gestión de evaluaciones"),
                ayuda,
                crearFormularioLayout(),
                crearBarraFiltro(),
                grid
        );
    }

    // ============================================================
    // FORMULARIO
    // ============================================================

    private void crearFormulario() {

        titulo.setWidthFull();
        titulo.setRequiredIndicatorVisible(true);
        titulo.setMaxLength(EvaluacionService.TITULO_MAXIMO);

        descripcion.setWidthFull();
        descripcion.setRequiredIndicatorVisible(true);
        descripcion.setMaxLength(EvaluacionService.MAX_DESCRIPCION);

        curso.setItemLabelGenerator(c ->
                c.getEstado() == Curso.EstadoCurso.inactivo
                        ? c.getTitulo() + " (inactivo)"
                        : c.getTitulo());
        curso.setWidthFull();
        curso.setRequiredIndicatorVisible(true);
        curso.setPlaceholder("Selecciona un curso");

        porcentajeAprobacion.setMin(EvaluacionService.PORCENTAJE_MINIMO);
        porcentajeAprobacion.setMax(EvaluacionService.PORCENTAJE_MAXIMO);
        porcentajeAprobacion.setStepButtonsVisible(true);
        porcentajeAprobacion.setValue(70);
        porcentajeAprobacion.setWidthFull();
        porcentajeAprobacion.setRequiredIndicatorVisible(true);

        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardar.addClickListener(event -> guardarEvaluacion());
        cancelar.addClickListener(event -> limpiarFormulario());
    }

    private HorizontalLayout crearFormularioLayout() {

        VerticalLayout formulario = new VerticalLayout();

        formulario.setPadding(true);
        formulario.setSpacing(true);
        formulario.setWidth("100%");
        formulario.setMaxWidth("850px");

        HorizontalLayout botones = new HorizontalLayout(guardar, cancelar);
        botones.setSpacing(true);

        formulario.add(
                titulo,
                descripcion,
                curso,
                porcentajeAprobacion,
                botones
        );

        HorizontalLayout contenedor = new HorizontalLayout(formulario);
        contenedor.setWidthFull();

        return contenedor;
    }

    private HorizontalLayout crearBarraFiltro() {

        filtro.setPlaceholder("Buscar por título o curso");
        filtro.setPrefixComponent(VaadinIcon.SEARCH.create());
        filtro.setClearButtonVisible(true);
        filtro.setValueChangeMode(ValueChangeMode.LAZY);
        filtro.setWidth("320px");
        filtro.addValueChangeListener(event -> aplicarFiltro());

        HorizontalLayout barra = new HorizontalLayout(filtro);
        barra.setWidthFull();
        return barra;
    }

    // ============================================================
    // TABLA
    // ============================================================

    private void configurarTabla() {

        grid.addColumn(Evaluacion::getId)
                .setHeader("ID")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(Evaluacion::getTitulo)
                .setHeader("Título")
                .setFlexGrow(2);

        grid.addColumn(this::nombreCurso)
                .setHeader("Curso")
                .setFlexGrow(2);

        grid.addColumn(e -> preguntasPorEvaluacion.getOrDefault(e.getId(), 0L))
                .setHeader("Preguntas")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(e -> e.getPorcentajeAprobacion() + " %")
                .setHeader("Aprobación")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(this::crearEtiquetaEstado)
                .setHeader("Estado")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(this::crearAcciones)
                .setHeader("Acciones")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.setWidthFull();
        grid.setEmptyStateText("No hay evaluaciones registradas.");
    }

    private Span crearEtiquetaEstado(Evaluacion evaluacion) {

        boolean activa = evaluacion.getEstado() == Evaluacion.EstadoEvaluacion.activo;

        Span estado = new Span(activa ? "Activa" : "Inactiva");
        estado.getElement().getThemeList().add(activa ? "badge success" : "badge contrast");
        return estado;
    }

    private HorizontalLayout crearAcciones(Evaluacion evaluacion) {

        Button editar = new Button("Editar", VaadinIcon.EDIT.create());
        editar.addThemeVariants(ButtonVariant.LUMO_SMALL);
        editar.addClickListener(event -> cargarEvaluacion(evaluacion));

        Button preguntas = new Button("Preguntas", VaadinIcon.LIST.create());
        preguntas.addThemeVariants(ButtonVariant.LUMO_SMALL);
        preguntas.addClickListener(event ->
                UI.getCurrent().navigate(PreguntasView.class, evaluacion.getId()));

        boolean activa = evaluacion.getEstado() == Evaluacion.EstadoEvaluacion.activo;

        Button estado = new Button(activa ? "Desactivar" : "Activar");
        estado.addThemeVariants(
                ButtonVariant.LUMO_SMALL,
                activa ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS);
        estado.addClickListener(event -> cambiarEstado(evaluacion));

        HorizontalLayout acciones = new HorizontalLayout(editar, preguntas, estado);
        acciones.setSpacing(true);
        acciones.setAlignItems(FlexComponent.Alignment.CENTER);
        return acciones;
    }

    private String nombreCurso(Evaluacion evaluacion) {

        if (evaluacion.getCursoId() == null) {
            return "Sin curso";
        }

        Curso c = cursosPorId.get(evaluacion.getCursoId());
        return c == null ? "Curso no encontrado" : c.getTitulo();
    }

    private void actualizarTabla() {

        List<Curso> cursos = cursoRepository.findAll();
        cursosPorId = cursos.stream()
                .collect(Collectors.toMap(Curso::getId, Function.identity()));

        Curso seleccionado = curso.getValue();
        curso.setItems(cursos);
        if (seleccionado != null) {
            curso.setValue(cursosPorId.get(seleccionado.getId()));
        }

        preguntasPorEvaluacion = evaluacionService.contarPreguntasPorEvaluacion();
        evaluaciones = evaluacionService.listarEvaluaciones(null);

        aplicarFiltro();
    }

    private void aplicarFiltro() {

        String texto = filtro.getValue() == null
                ? ""
                : filtro.getValue().trim().toLowerCase(Locale.ROOT);

        if (texto.isEmpty()) {
            grid.setItems(evaluaciones);
            return;
        }

        grid.setItems(evaluaciones.stream()
                .filter(e -> e.getTitulo().toLowerCase(Locale.ROOT).contains(texto)
                        || nombreCurso(e).toLowerCase(Locale.ROOT).contains(texto))
                .toList());
    }

    // ============================================================
    // ACCIONES
    // ============================================================

    private void guardarEvaluacion() {

        try {
            boolean nueva = evaluacionActual == null;

            Evaluacion guardada = evaluacionService.guardarEvaluacion(
                    nueva ? null : evaluacionActual.getId(),
                    curso.getValue() == null ? null : curso.getValue().getId(),
                    titulo.getValue(),
                    descripcion.getValue(),
                    porcentajeAprobacion.getValue()
            );

            mostrarExito(nueva
                    ? "Evaluación creada. Ahora agrega sus preguntas."
                    : "Evaluación actualizada correctamente.");

            limpiarFormulario();
            actualizarTabla();

            if (nueva) {
                UI.getCurrent().navigate(PreguntasView.class, guardada.getId());
            }

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void cambiarEstado(Evaluacion evaluacion) {

        try {
            Evaluacion actualizada = evaluacionService.cambiarEstado(evaluacion.getId());

            mostrarExito(actualizada.getEstado() == Evaluacion.EstadoEvaluacion.activo
                    ? "Evaluación activada correctamente."
                    : "Evaluación desactivada correctamente.");

            actualizarTabla();

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void cargarEvaluacion(Evaluacion evaluacion) {

        evaluacionActual = evaluacion;

        titulo.setValue(evaluacion.getTitulo());
        descripcion.setValue(evaluacion.getDescripcion());
        curso.setValue(cursosPorId.get(evaluacion.getCursoId()));
        porcentajeAprobacion.setValue(evaluacion.getPorcentajeAprobacion());

        guardar.setText("Actualizar evaluación");
        titulo.focus();
    }

    private void limpiarFormulario() {

        evaluacionActual = null;

        titulo.clear();
        descripcion.clear();
        curso.clear();
        porcentajeAprobacion.setValue(70);

        guardar.setText("Guardar evaluación");
    }

    private void mostrarExito(String mensaje) {
        Notification.show(mensaje, 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void mostrarError(String mensaje) {
        Notification.show(mensaje, 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
