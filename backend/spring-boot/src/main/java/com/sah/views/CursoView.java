package com.sah.views;

import com.sah.model.Curso;
import com.sah.service.CursoService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.Locale;

@Route(value = "cursos", layout = MainLayout.class)
@PageTitle("Gestión de cursos | SAH")
@RolesAllowed("ADMIN")
public class CursoView extends VerticalLayout {

    private final CursoService cursoService;

    private final Grid<Curso> grid = new Grid<>(Curso.class, false);
    private final Binder<Curso> binder = new Binder<>(Curso.class);

    private final TextField buscador = new TextField();
    private final ComboBox<Curso.EstadoCurso> filtroEstado =
            new ComboBox<>();

    // Campos del formulario
    private final TextField titulo = new TextField("Título del curso");
    private final TextArea descripcion = new TextArea("Descripción");
    private final ComboBox<Curso.NivelDificultad> nivel =
            new ComboBox<>("Nivel de dificultad");
    private final IntegerField duracion = new IntegerField("Duración (horas)");
    private final IntegerField modulos = new IntegerField("Número de módulos");
    private final IntegerField porcentaje =
            new IntegerField("Porcentaje de aprobación (%)");

    private List<Curso> cursos = List.of();

    public CursoView(CursoService cursoService) {

        this.cursoService = cursoService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configurarFormulario();
        configurarGrid();

        add(crearEncabezado(), crearFiltros(), grid);

        actualizarGrid();
    }

    private VerticalLayout crearEncabezado() {

        H2 tituloVista = new H2("Gestión de cursos");

        Button nuevo = new Button(
                "Registrar curso",
                event -> abrirFormulario(null)
        );
        nuevo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout encabezado = new HorizontalLayout(tituloVista, nuevo);
        encabezado.setWidthFull();
        encabezado.setAlignItems(FlexComponent.Alignment.CENTER);
        encabezado.expand(tituloVista);

        Paragraph descripcionVista = new Paragraph(
                "Administra los cursos de capacitación en ciberseguridad. "
                        + "Los cursos no se eliminan: se desactivan para "
                        + "conservar sus evaluaciones y asignaciones."
        );

        VerticalLayout contenedor = new VerticalLayout(encabezado, descripcionVista);
        contenedor.setPadding(false);
        contenedor.setSpacing(false);
        contenedor.setWidthFull();

        return contenedor;
    }

    private HorizontalLayout crearFiltros() {

        buscador.setPlaceholder("Buscar por título");
        buscador.setClearButtonVisible(true);
        buscador.setValueChangeMode(ValueChangeMode.LAZY);
        buscador.addValueChangeListener(event -> aplicarFiltros());
        buscador.setWidth("320px");

        filtroEstado.setPlaceholder("Todos los estados");
        filtroEstado.setItems(Curso.EstadoCurso.values());
        filtroEstado.setItemLabelGenerator(this::nombreEstado);
        filtroEstado.setClearButtonVisible(true);
        filtroEstado.addValueChangeListener(event -> aplicarFiltros());

        HorizontalLayout filtros = new HorizontalLayout(buscador, filtroEstado);
        filtros.setAlignItems(FlexComponent.Alignment.BASELINE);
        filtros.getStyle().set("flex-wrap", "wrap");

        return filtros;
    }

    private void configurarFormulario() {

        binder.forField(titulo)
                .asRequired("El título es obligatorio")
                .withValidator(
                        valor -> valor.trim().length() <= CursoService.TITULO_MAXIMO,
                        "Máximo " + CursoService.TITULO_MAXIMO + " caracteres"
                )
                .bind(Curso::getTitulo, Curso::setTitulo);

        binder.forField(descripcion)
                .asRequired("La descripción es obligatoria")
                .bind(Curso::getDescripcion, Curso::setDescripcion);

        nivel.setItems(Curso.NivelDificultad.values());
        nivel.setItemLabelGenerator(this::nombreNivel);

        binder.forField(nivel)
                .asRequired("Selecciona el nivel")
                .bind(Curso::getNivelDificultad, Curso::setNivelDificultad);

        configurarNumero(duracion, 1, CursoService.DURACION_MAXIMA_HORAS);
        binder.forField(duracion)
                .asRequired("La duración es obligatoria")
                .withValidator(
                        valor -> valor >= 1
                                && valor <= CursoService.DURACION_MAXIMA_HORAS,
                        "Entre 1 y " + CursoService.DURACION_MAXIMA_HORAS + " horas"
                )
                .bind(Curso::getDuracion, Curso::setDuracion);

        configurarNumero(modulos, 1, CursoService.MODULOS_MAXIMOS);
        binder.forField(modulos)
                .asRequired("El número de módulos es obligatorio")
                .withValidator(
                        valor -> valor >= 1 && valor <= CursoService.MODULOS_MAXIMOS,
                        "Entre 1 y " + CursoService.MODULOS_MAXIMOS + " módulos"
                )
                .bind(Curso::getNumeroModulos, Curso::setNumeroModulos);

        configurarNumero(porcentaje, 1, 100);
        binder.forField(porcentaje)
                .asRequired("El porcentaje es obligatorio")
                .withValidator(
                        valor -> valor >= 1 && valor <= 100,
                        "Entre 1 y 100"
                )
                .bind(Curso::getPorcentajeAprobacion,
                        Curso::setPorcentajeAprobacion);

        titulo.setMaxLength(CursoService.TITULO_MAXIMO);
        descripcion.setMinHeight("120px");

        for (var campo : List.of(titulo, descripcion)) {
            campo.setWidthFull();
        }
        nivel.setWidthFull();
        duracion.setWidthFull();
        modulos.setWidthFull();
        porcentaje.setWidthFull();
    }

    private void configurarNumero(IntegerField campo, int minimo, int maximo) {
        campo.setMin(minimo);
        campo.setMax(maximo);
        campo.setStepButtonsVisible(true);
    }

    private void configurarGrid() {

        grid.addColumn(Curso::getId)
                .setHeader("ID")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(Curso::getTitulo)
                .setHeader("Curso")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);

        grid.addColumn(curso -> nombreNivel(curso.getNivelDificultad()))
                .setHeader("Nivel")
                .setAutoWidth(true);

        grid.addColumn(curso -> curso.getDuracion() + " h")
                .setHeader("Duración")
                .setAutoWidth(true);

        grid.addColumn(Curso::getNumeroModulos)
                .setHeader("Módulos")
                .setAutoWidth(true);

        grid.addColumn(curso -> curso.getPorcentajeAprobacion() + " %")
                .setHeader("Aprobación")
                .setAutoWidth(true);

        grid.addComponentColumn(this::crearEtiquetaEstado)
                .setHeader("Estado")
                .setAutoWidth(true);

        grid.addComponentColumn(curso -> {

            Button editar = new Button("Editar", event -> abrirFormulario(curso));
            editar.addThemeVariants(ButtonVariant.LUMO_SMALL);

            boolean activo = curso.getEstado() == Curso.EstadoCurso.activo;

            Button estado = new Button(
                    activo ? "Desactivar" : "Activar",
                    event -> confirmarCambioEstado(curso)
            );
            estado.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    activo ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS
            );

            return new HorizontalLayout(editar, estado);

        }).setHeader("Acciones").setAutoWidth(true);

        grid.setSizeFull();
    }

    private Span crearEtiquetaEstado(Curso curso) {

        boolean activo = curso.getEstado() == Curso.EstadoCurso.activo;

        Span etiqueta = new Span(nombreEstado(curso.getEstado()));
        etiqueta.getElement().getThemeList().add(
                activo ? "badge success" : "badge contrast"
        );

        return etiqueta;
    }

    private void abrirFormulario(Curso curso) {

        boolean esNuevo = curso == null;

        // Se trabaja sobre una copia para no alterar la fila si se cancela
        Curso modelo = new Curso();
        if (!esNuevo) {
            modelo.setTitulo(curso.getTitulo());
            modelo.setDescripcion(curso.getDescripcion());
            modelo.setNivelDificultad(curso.getNivelDificultad());
            modelo.setDuracion(curso.getDuracion());
            modelo.setNumeroModulos(curso.getNumeroModulos());
            modelo.setPorcentajeAprobacion(curso.getPorcentajeAprobacion());
        } else {
            modelo.setNivelDificultad(Curso.NivelDificultad.basico);
            modelo.setNumeroModulos(1);
            modelo.setPorcentajeAprobacion(70);
        }

        binder.readBean(modelo);

        Dialog dialogo = new Dialog();
        dialogo.setHeaderTitle(esNuevo ? "Registrar curso" : "Editar curso");
        dialogo.setWidth("600px");
        dialogo.setMaxWidth("95vw");

        HorizontalLayout numeros = new HorizontalLayout(duracion, modulos, porcentaje);
        numeros.setWidthFull();
        numeros.getStyle().set("flex-wrap", "wrap");

        VerticalLayout formulario = new VerticalLayout(
                titulo, descripcion, nivel, numeros
        );
        formulario.setPadding(false);

        Button guardar = new Button("Guardar", event -> {
            try {
                binder.writeBean(modelo);

                if (esNuevo) {
                    cursoService.registrarCurso(modelo);
                } else {
                    cursoService.actualizarCurso(curso.getId(), modelo);
                }

                mostrarExito("Curso guardado correctamente.");
                dialogo.close();
                actualizarGrid();

            } catch (ValidationException e) {
                mostrarError("Revisa los campos marcados en el formulario.");
            } catch (IllegalArgumentException e) {
                mostrarError(e.getMessage());
            } catch (Exception e) {
                mostrarError("No fue posible guardar el curso.");
            }
        });
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelar = new Button("Cancelar", event -> dialogo.close());

        dialogo.add(formulario);
        dialogo.getFooter().add(cancelar, guardar);
        dialogo.open();
    }

    private void confirmarCambioEstado(Curso curso) {

        boolean activo = curso.getEstado() == Curso.EstadoCurso.activo;

        Dialog confirmacion = new Dialog();
        confirmacion.setHeaderTitle(
                activo ? "Desactivar curso" : "Activar curso"
        );

        confirmacion.add(new Paragraph(
                activo
                        ? "El curso «" + curso.getTitulo() + "» dejará de estar "
                                + "disponible para nuevas asignaciones. Las "
                                + "asignaciones existentes se conservan."
                        : "El curso «" + curso.getTitulo() + "» volverá a estar "
                                + "disponible para asignaciones."
        ));

        Button aceptar = new Button(activo ? "Desactivar" : "Activar", event -> {
            try {
                cursoService.cambiarEstado(curso.getId());
                mostrarExito(activo ? "Curso desactivado." : "Curso activado.");
                confirmacion.close();
                actualizarGrid();
            } catch (IllegalArgumentException e) {
                mostrarError(e.getMessage());
            }
        });
        aceptar.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                activo ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS
        );

        Button cancelar = new Button("Cancelar", event -> confirmacion.close());

        confirmacion.getFooter().add(cancelar, aceptar);
        confirmacion.open();
    }

    private void actualizarGrid() {
        cursos = cursoService.listarCursos();
        aplicarFiltros();
    }

    private void aplicarFiltros() {

        String texto = buscador.getValue() == null
                ? ""
                : buscador.getValue().trim().toLowerCase(Locale.ROOT);

        Curso.EstadoCurso estado = filtroEstado.getValue();

        grid.setItems(cursos.stream()
                .filter(curso -> texto.isEmpty()
                        || curso.getTitulo().toLowerCase(Locale.ROOT).contains(texto))
                .filter(curso -> estado == null || curso.getEstado() == estado)
                .toList());
    }

    private void mostrarExito(String mensaje) {
        Notification.show(mensaje, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void mostrarError(String mensaje) {
        Notification.show(mensaje, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private String nombreNivel(Curso.NivelDificultad nivelCurso) {
        if (nivelCurso == null) {
            return "";
        }
        return switch (nivelCurso) {
            case basico -> "Básico";
            case intermedio -> "Intermedio";
            case avanzado -> "Avanzado";
        };
    }

    private String nombreEstado(Curso.EstadoCurso estado) {
        if (estado == null) {
            return "";
        }
        return switch (estado) {
            case activo -> "Activo";
            case inactivo -> "Inactivo";
        };
    }
}
