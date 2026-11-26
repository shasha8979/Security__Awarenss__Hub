package com.sah.views;

import com.sah.dto.AvanceEstudianteDTO;
import com.sah.model.Empresa;
import com.sah.model.Usuario;
import com.sah.repository.UsuarioRepository;
import com.sah.service.ReporteService;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import jakarta.annotation.security.RolesAllowed;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Locale;

/**
 * Seguimiento de los estudiantes de la empresa del administrador
 * autenticado. La empresa se obtiene siempre del usuario en sesion,
 * por lo que no es posible consultar datos de otras empresas.
 */
@Route(value = "dashboard-empresa", layout = MainLayout.class)
@PageTitle("Seguimiento de estudiantes | SAH")
@RolesAllowed("ADMIN_EMPRESA")
public class AdminEmpresaView extends VerticalLayout {

    private final UsuarioRepository usuarioRepository;
    private final ReporteService reporteService;

    private final Grid<AvanceEstudianteDTO> grid =
            new Grid<>(AvanceEstudianteDTO.class, false);
    private final TextField buscador = new TextField();

    private List<AvanceEstudianteDTO> estudiantes = List.of();

    public AdminEmpresaView(
            UsuarioRepository usuarioRepository,
            ReporteService reporteService) {

        this.usuarioRepository = usuarioRepository;
        this.reporteService = reporteService;

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        crearContenido();
    }

    private void crearContenido() {

        H2 titulo = new H2("Seguimiento de estudiantes");

        Usuario usuario = obtenerUsuarioActual();
        Empresa empresa = usuario == null ? null : usuario.getEmpresa();

        if (empresa == null) {
            add(titulo, new Paragraph(
                    "Tu cuenta no tiene una empresa asignada. "
                            + "Solicita al administrador del sistema que "
                            + "asocie tu usuario a una empresa."
            ));
            return;
        }

        Paragraph descripcion = new Paragraph(
                "Avance de capacitación de los estudiantes de "
                        + empresa.getNombre() + "."
        );
        descripcion.getStyle().set("margin", "0").set("color", "#64748B");

        estudiantes = reporteService.generarAvancePorEstudiante(empresa.getId());

        add(
                titulo,
                descripcion,
                crearIndicadores(),
                crearSeccionTabla()
        );
    }

    private Component crearIndicadores() {

        long total = estudiantes.size();
        long activos = estudiantes.stream()
                .filter(e -> "Activo".equals(e.estado()))
                .count();
        long asignaciones = estudiantes.stream()
                .mapToLong(AvanceEstudianteDTO::asignaciones).sum();
        long completadas = estudiantes.stream()
                .mapToLong(AvanceEstudianteDTO::completadas).sum();
        long sinCapacitacion = estudiantes.stream()
                .filter(e -> e.asignaciones() == 0)
                .count();

        Div indicadores = new Div(
                crearIndicador("Estudiantes", String.valueOf(total),
                        activos + " activos"),
                crearIndicador("Asignaciones", String.valueOf(asignaciones),
                        completadas + " completadas"),
                crearIndicador("Avance general",
                        (asignaciones == 0
                                ? 0
                                : Math.round(completadas * 100.0 / asignaciones))
                                + " %",
                        "de las asignaciones completadas"),
                crearIndicador("Sin capacitación", String.valueOf(sinCapacitacion),
                        "estudiantes sin asignaciones")
        );

        indicadores.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(200px, 1fr))")
                .set("gap", "16px")
                .set("width", "100%");

        return indicadores;
    }

    private Component crearSeccionTabla() {

        H3 subtitulo = new H3("Estudiantes");
        subtitulo.getStyle().set("margin", "16px 0 0 0").set("color", "#1769AA");

        buscador.setPlaceholder("Buscar por nombre o correo");
        buscador.setClearButtonVisible(true);
        buscador.setValueChangeMode(ValueChangeMode.LAZY);
        buscador.addValueChangeListener(event -> aplicarFiltro());
        buscador.setWidth("320px");
        buscador.setMaxWidth("100%");

        RouterLink asignar = new RouterLink("Asignar capacitaciones", AsignacionView.class);

        HorizontalLayout barra = new HorizontalLayout(buscador, asignar);
        barra.setAlignItems(FlexComponent.Alignment.CENTER);
        barra.setWidthFull();
        barra.getStyle().set("flex-wrap", "wrap");

        configurarGrid();
        aplicarFiltro();

        VerticalLayout seccion = new VerticalLayout(subtitulo, barra, grid);
        seccion.setPadding(false);
        seccion.setWidthFull();

        if (estudiantes.isEmpty()) {
            seccion.add(new Paragraph(
                    "Tu empresa todavía no tiene estudiantes registrados."
            ));
        }

        return seccion;
    }

    private void configurarGrid() {

        grid.addColumn(AvanceEstudianteDTO::nombre)
                .setHeader("Estudiante")
                .setFlexGrow(1)
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(AvanceEstudianteDTO::correo)
                .setHeader("Correo")
                .setAutoWidth(true);

        grid.addColumn(AvanceEstudianteDTO::estado)
                .setHeader("Estado")
                .setAutoWidth(true);

        grid.addColumn(AvanceEstudianteDTO::asignaciones)
                .setHeader("Asignadas")
                .setAutoWidth(true);

        grid.addColumn(AvanceEstudianteDTO::pendientes)
                .setHeader("Pendientes")
                .setAutoWidth(true);

        grid.addColumn(AvanceEstudianteDTO::enProgreso)
                .setHeader("En progreso")
                .setAutoWidth(true);

        grid.addColumn(AvanceEstudianteDTO::completadas)
                .setHeader("Completadas")
                .setAutoWidth(true);

        grid.addComponentColumn(this::crearBarra)
                .setHeader("% completado")
                .setWidth("180px")
                .setFlexGrow(0);

        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    private void aplicarFiltro() {

        String texto = buscador.getValue() == null
                ? ""
                : buscador.getValue().trim().toLowerCase(Locale.ROOT);

        grid.setItems(estudiantes.stream()
                .filter(e -> texto.isEmpty()
                        || e.nombre().toLowerCase(Locale.ROOT).contains(texto)
                        || e.correo().toLowerCase(Locale.ROOT).contains(texto))
                .toList());
    }

    private Component crearBarra(AvanceEstudianteDTO fila) {

        if (fila.asignaciones() == 0) {
            Span sinDatos = new Span("Sin asignaciones");
            sinDatos.getStyle().set("font-size", "12px").set("color", "#94A3B8");
            return sinDatos;
        }

        ProgressBar barra = new ProgressBar(0, 100, fila.porcentajeCompletado());
        barra.setWidth("90px");

        Span valor = new Span(fila.porcentajeCompletado() + " %");
        valor.getStyle().set("font-size", "12px");

        HorizontalLayout contenedor = new HorizontalLayout(barra, valor);
        contenedor.setAlignItems(FlexComponent.Alignment.CENTER);
        return contenedor;
    }

    private Div crearIndicador(String titulo, String valor, String detalle) {

        Span etiqueta = new Span(titulo);
        etiqueta.getStyle()
                .set("font-size", "13px")
                .set("font-weight", "600")
                .set("color", "#475569");

        Span numero = new Span(valor);
        numero.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "800")
                .set("color", "#1769AA");

        Span descripcion = new Span(detalle);
        descripcion.getStyle().set("font-size", "12px").set("color", "#64748B");

        Div tarjeta = new Div(etiqueta, numero, descripcion);
        tarjeta.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px")
                .set("padding", "16px")
                .set("background", "#FFFFFF")
                .set("border", "1px solid #E2E8F0")
                .set("border-radius", "12px");

        return tarjeta;
    }

    private Usuario obtenerUsuarioActual() {

        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion == null) {
            return null;
        }

        // findByCorreo no carga la empresa (relacion LAZY); findById tiene
        // EntityGraph con "empresa", asi se evita LazyInitializationException.
        return usuarioRepository.findByCorreo(autenticacion.getName())
                .flatMap(u -> usuarioRepository.findById(u.getId()))
                .orElse(null);
    }
}
