
package com.sah.views;

import com.sah.entity.Evaluacion;
import com.sah.entity.Curso;
import com.sah.repository.EvaluacionRepository;
import com.sah.repository.CursoRepository;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route("evaluaciones")
@PageTitle("Evaluaciones | Security Awareness Hub")
@RolesAllowed("ADMIN")
public class EvaluacionView extends MainLayout  {

    private final EvaluacionRepository evaluacionRepository;
    private final CursoRepository cursoRepository;

    private final Grid<Evaluacion> grid = new Grid<>(Evaluacion.class, false);

    private final TextField titulo = new TextField("Título");
    private final TextArea descripcion = new TextArea("Descripción");
    private final ComboBox<Curso> curso = new ComboBox<>("Curso asociado");
    private final IntegerField porcentajeAprobacion =
            new IntegerField("Porcentaje de aprobación");

    private final Button guardar = new Button("Guardar evaluación");
    private final Button cancelar = new Button("Cancelar");

    private Evaluacion evaluacionActual;

    public EvaluacionView(
            EvaluacionRepository evaluacionRepository,
            CursoRepository cursoRepository) {

        this.evaluacionRepository = evaluacionRepository;
        this.cursoRepository = cursoRepository;
crearFormulario();
configurarTabla();
actualizarTabla();

VerticalLayout contenido = new VerticalLayout(
        new H1("Gestión de evaluaciones"),
        new Paragraph("Administra las evaluaciones de los cursos."),
        crearFormularioLayout(),
        grid
);

contenido.setSizeFull();
contenido.setPadding(true);
contenido.setSpacing(true);

setContent(contenido);
    }

    private void crearFormulario() {

        titulo.setWidthFull();

        descripcion.setWidthFull();
        descripcion.setMaxLength(5000);

        curso.setItems(cursoRepository.findAll());
        curso.setItemLabelGenerator(Curso::getTitulo);
        curso.setWidthFull();
        curso.setPlaceholder("Selecciona un curso");

        porcentajeAprobacion.setMin(0);
        porcentajeAprobacion.setMax(100);
        porcentajeAprobacion.setValue(70);
        porcentajeAprobacion.setWidthFull();

        guardar.addClickListener(event -> guardarEvaluacion());
        cancelar.addClickListener(event -> limpiarFormulario());
    }

   private HorizontalLayout crearFormularioLayout() {

    VerticalLayout formulario = new VerticalLayout();

    formulario.setPadding(true);
    formulario.setSpacing(true);
    formulario.setWidth("100%");
    formulario.setMaxWidth("850px");

    titulo.setWidthFull();
    descripcion.setWidthFull();
    curso.setWidthFull();
    porcentajeAprobacion.setWidthFull();

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

    private void configurarTabla() {

        grid.addColumn(Evaluacion::getId)
                .setHeader("ID");

        grid.addColumn(Evaluacion::getTitulo)
                .setHeader("Título");

        grid.addColumn(evaluacion ->
                evaluacion.getCursoId() == null
                        ? "Sin curso"
                        : cursoRepository.findById(evaluacion.getCursoId())
                                .map(Curso::getTitulo)
                                .orElse("Curso no encontrado"))
                .setHeader("Curso");

        grid.addColumn(Evaluacion::getPorcentajeAprobacion)
                .setHeader("Aprobación (%)");

        grid.addColumn(evaluacion ->
                evaluacion.getEstado().name())
                .setHeader("Estado");

        grid.addComponentColumn(evaluacion -> {

            Button editar = new Button("Editar");

            editar.addClickListener(event ->
                    cargarEvaluacion(evaluacion));

            return editar;

        }).setHeader("Acciones");

        grid.setWidthFull();
    }

    private void actualizarTabla() {
        grid.setItems(evaluacionRepository.findAll());
    }

    private void guardarEvaluacion() {

        if (titulo.isEmpty()
                || descripcion.isEmpty()
                || curso.isEmpty()
                || porcentajeAprobacion.isEmpty()) {

            Notification.show(
                    "Completa todos los campos obligatorios."
            );

            return;
        }

        Evaluacion evaluacion = evaluacionActual == null
                ? new Evaluacion()
                : evaluacionActual;

        evaluacion.setTitulo(titulo.getValue().trim());
        evaluacion.setDescripcion(descripcion.getValue().trim());
        evaluacion.setCursoId(curso.getValue().getId());
        evaluacion.setPorcentajeAprobacion(
                porcentajeAprobacion.getValue()
        );

        if (evaluacionActual == null) {
            evaluacion.setEstado(Evaluacion.EstadoEvaluacion.activo);
        }

        evaluacionRepository.save(evaluacion);

        Notification.show("Evaluación guardada correctamente.");

        limpiarFormulario();
        actualizarTabla();
    }

    private void cargarEvaluacion(Evaluacion evaluacion) {

        evaluacionActual = evaluacion;

        titulo.setValue(evaluacion.getTitulo());
        descripcion.setValue(evaluacion.getDescripcion());

        cursoRepository.findById(evaluacion.getCursoId())
                .ifPresent(curso::setValue);

        porcentajeAprobacion.setValue(
                evaluacion.getPorcentajeAprobacion()
        );

        guardar.setText("Actualizar evaluación");
    }

    private void limpiarFormulario() {

        evaluacionActual = null;

        titulo.clear();
        descripcion.clear();
        curso.clear();

        porcentajeAprobacion.setValue(70);

        guardar.setText("Guardar evaluación");
    }
}
