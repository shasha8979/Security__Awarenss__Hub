package com.sah.views;

import com.sah.dto.AvanceCursoDTO;
import com.sah.dto.AvanceEmpresaDTO;
import com.sah.dto.ResumenReporteDTO;
import com.sah.service.ReporteService;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "reportes", layout = MainLayout.class)
@PageTitle("Reportes | SAH")
@RolesAllowed("ADMIN")
public class ReporteView extends VerticalLayout {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReporteService reporteService;

    private final Div indicadores = new Div();
    private final Grid<AvanceCursoDTO> gridCursos =
            new Grid<>(AvanceCursoDTO.class, false);
    private final Grid<AvanceEmpresaDTO> gridEmpresas =
            new Grid<>(AvanceEmpresaDTO.class, false);
    private final Span fechaGeneracion = new Span();

    public ReporteView(ReporteService reporteService) {

        this.reporteService = reporteService;

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        indicadores.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(200px, 1fr))")
                .set("gap", "16px")
                .set("width", "100%");

        configurarGridCursos();
        configurarGridEmpresas();

        add(
                crearEncabezado(),
                indicadores,
                crearSeccion(
                        "Avance por curso",
                        "Asignaciones de tipo curso agrupadas por su estado.",
                        gridCursos
                ),
                crearSeccion(
                        "Avance por empresa",
                        "Estudiantes y asignaciones (cursos y evaluaciones) "
                                + "de cada empresa.",
                        gridEmpresas
                ),
                crearAvisoResultados()
        );

        cargarDatos();
    }

    private Component crearEncabezado() {

        H2 titulo = new H2("Reportes");

        Button actualizar = new Button(
                "Actualizar",
                VaadinIcon.REFRESH.create(),
                event -> cargarDatos()
        );

        HorizontalLayout fila = new HorizontalLayout(titulo, actualizar);
        fila.setWidthFull();
        fila.setAlignItems(FlexComponent.Alignment.CENTER);
        fila.expand(titulo);

        Paragraph descripcion = new Paragraph(
                "Indicadores de capacitación calculados con los datos "
                        + "registrados en la plataforma."
        );
        descripcion.getStyle().set("margin", "0");

        fechaGeneracion.getStyle()
                .set("font-size", "12px")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout encabezado = new VerticalLayout(fila, descripcion, fechaGeneracion);
        encabezado.setPadding(false);
        encabezado.setSpacing(false);

        return encabezado;
    }

    private void cargarDatos() {

        ResumenReporteDTO resumen = reporteService.generarResumen();

        indicadores.removeAll();
        indicadores.add(
                crearTarjeta("Empresas",
                        resumen.empresasActivas() + " activas de "
                                + resumen.empresasTotal(),
                        String.valueOf(resumen.empresasTotal())),
                crearTarjeta("Usuarios",
                        resumen.administradores() + " admin · "
                                + resumen.administradoresEmpresa()
                                + " admin empresa · "
                                + resumen.estudiantes() + " estudiantes",
                        String.valueOf(resumen.usuariosTotal())),
                crearTarjeta("Estudiantes activos",
                        "de " + resumen.estudiantes() + " estudiantes",
                        String.valueOf(resumen.estudiantesActivos())),
                crearTarjeta("Cursos",
                        resumen.cursosActivos() + " activos de "
                                + resumen.cursosTotal(),
                        String.valueOf(resumen.cursosTotal())),
                crearTarjeta("Evaluaciones",
                        resumen.evaluacionesActivas() + " activas de "
                                + resumen.evaluacionesTotal(),
                        String.valueOf(resumen.evaluacionesTotal())),
                crearTarjeta("Asignaciones",
                        resumen.asignacionesPendientes() + " pendientes · "
                                + resumen.asignacionesEnProgreso()
                                + " en progreso · "
                                + resumen.asignacionesCompletadas()
                                + " completadas",
                        String.valueOf(resumen.asignacionesTotal())),
                crearTarjeta("Completado general",
                        resumen.asignacionesCompletadas() + " de "
                                + resumen.asignacionesTotal()
                                + " asignaciones",
                        resumen.porcentajeCompletado() + " %")
        );

        List<AvanceCursoDTO> cursos = reporteService.generarAvancePorCurso();
        gridCursos.setItems(cursos);

        List<AvanceEmpresaDTO> empresas = reporteService.generarAvancePorEmpresa();
        gridEmpresas.setItems(empresas);

        fechaGeneracion.setText(
                "Generado el " + LocalDateTime.now().format(FORMATO_FECHA)
        );
    }

    private Div crearTarjeta(String titulo, String detalle, String valor) {

        Span etiqueta = new Span(titulo);
        etiqueta.getStyle()
                .set("font-size", "13px")
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)");

        Span numero = new Span(valor);
        numero.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "800")
                .set("color", "var(--lumo-primary-text-color)");

        Span descripcion = new Span(detalle);
        descripcion.getStyle()
                .set("font-size", "12px")
                .set("color", "var(--lumo-secondary-text-color)");

        Div tarjeta = new Div(etiqueta, numero, descripcion);
        tarjeta.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px")
                .set("padding", "16px")
                .set("background", "var(--sah-superficie)")
                .set("border", "1px solid var(--sah-borde)")
                .set("border-radius", "12px");

        return tarjeta;
    }

    private Component crearSeccion(String titulo, String descripcion, Grid<?> grid) {

        H3 encabezado = new H3(titulo);
        encabezado.getStyle()
                .set("margin", "16px 0 0 0")
                .set("color", "var(--lumo-primary-text-color)");

        Paragraph texto = new Paragraph(descripcion);
        texto.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)");

        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        VerticalLayout seccion = new VerticalLayout(encabezado, texto, grid);
        seccion.setPadding(false);
        seccion.setWidthFull();

        return seccion;
    }

    private void configurarGridCursos() {

        gridCursos.addColumn(AvanceCursoDTO::curso)
                .setHeader("Curso")
                .setFlexGrow(1)
                .setAutoWidth(true);

        gridCursos.addColumn(AvanceCursoDTO::estadoCurso)
                .setHeader("Estado")
                .setAutoWidth(true);

        gridCursos.addColumn(AvanceCursoDTO::asignados)
                .setHeader("Asignados")
                .setAutoWidth(true);

        gridCursos.addColumn(AvanceCursoDTO::pendientes)
                .setHeader("Pendientes")
                .setAutoWidth(true);

        gridCursos.addColumn(AvanceCursoDTO::enProgreso)
                .setHeader("En progreso")
                .setAutoWidth(true);

        gridCursos.addColumn(AvanceCursoDTO::completados)
                .setHeader("Completados")
                .setAutoWidth(true);

        gridCursos.addComponentColumn(fila ->
                        crearBarra(fila.porcentajeCompletado(), fila.asignados()))
                .setHeader("% completado")
                .setWidth("180px")
                .setFlexGrow(0);
    }

    private void configurarGridEmpresas() {

        gridEmpresas.addColumn(AvanceEmpresaDTO::empresa)
                .setHeader("Empresa")
                .setFlexGrow(1)
                .setAutoWidth(true);

        gridEmpresas.addColumn(AvanceEmpresaDTO::estudiantes)
                .setHeader("Estudiantes")
                .setAutoWidth(true);

        gridEmpresas.addColumn(AvanceEmpresaDTO::asignaciones)
                .setHeader("Asignaciones")
                .setAutoWidth(true);

        gridEmpresas.addColumn(AvanceEmpresaDTO::pendientes)
                .setHeader("Pendientes")
                .setAutoWidth(true);

        gridEmpresas.addColumn(AvanceEmpresaDTO::enProgreso)
                .setHeader("En progreso")
                .setAutoWidth(true);

        gridEmpresas.addColumn(AvanceEmpresaDTO::completadas)
                .setHeader("Completadas")
                .setAutoWidth(true);

        gridEmpresas.addComponentColumn(fila ->
                        crearBarra(fila.porcentajeCompletado(), fila.asignaciones()))
                .setHeader("% completado")
                .setWidth("180px")
                .setFlexGrow(0);
    }

    private Component crearBarra(int porcentaje, long total) {

        if (total == 0) {
            Span sinDatos = new Span("Sin asignaciones");
            sinDatos.getStyle()
                    .set("font-size", "12px")
                    .set("color", "var(--lumo-tertiary-text-color)");
            return sinDatos;
        }

        ProgressBar barra = new ProgressBar(0, 100, porcentaje);
        barra.setWidth("90px");

        Span valor = new Span(porcentaje + " %");
        valor.getStyle().set("font-size", "12px");

        HorizontalLayout contenedor = new HorizontalLayout(barra, valor);
        contenedor.setAlignItems(FlexComponent.Alignment.CENTER);
        return contenedor;
    }

    private Component crearAvisoResultados() {

        Paragraph aviso = new Paragraph(
                "Resultados de evaluaciones (calificaciones): pendiente. "
                        + "La base de datos todavía no registra las respuestas "
                        + "ni las notas de los estudiantes, por eso este reporte "
                        + "no muestra calificaciones."
        );

        aviso.getStyle()
                .set("padding", "12px 16px")
                .set("background", "var(--sah-aviso-fondo)")
                .set("border", "1px solid var(--sah-aviso-borde)")
                .set("border-radius", "8px")
                .set("color", "var(--sah-aviso-texto)")
                .set("font-size", "13px");

        return aviso;
    }
}
