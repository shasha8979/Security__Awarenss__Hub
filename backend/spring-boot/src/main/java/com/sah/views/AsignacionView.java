
package com.sah.views;

import com.sah.entity.Asignacion;
import com.sah.entity.Usuario;
import com.sah.entity.Curso;
import com.sah.entity.Evaluacion;

import com.sah.repository.AsignacionRepository;
import com.sah.repository.UsuarioRepository;
import com.sah.repository.CursoRepository;
import com.sah.repository.EvaluacionRepository;

import com.sah.service.AsignacionService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.ArrayList;
import java.util.List;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "asignaciones", layout = MainLayout.class)
@PageTitle("Asignaciones | Security Awareness Hub")
@RolesAllowed({"ADMIN", "ADMIN_EMPRESA", "EMPLEADO"})
public class AsignacionView extends VerticalLayout {

    private final AsignacionService asignacionService;
    private final AsignacionRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final EvaluacionRepository evaluacionRepository;

    private final Grid<Asignacion> tabla =
            new Grid<>(Asignacion.class, false);

    // FORMULARIO

    private final ComboBox<Usuario> empleado =
            new ComboBox<>("Seleccionar empleado");

    private final ComboBox<Asignacion.Tipo> tipo =
            new ComboBox<>("Tipo de asignación");

    private final ComboBox<Curso> curso =
            new ComboBox<>("Seleccionar curso");

    private final ComboBox<Evaluacion> evaluacion =
            new ComboBox<>("Seleccionar evaluación");

    private final ComboBox<Asignacion.Estado> filtroEstado =
            new ComboBox<>("Filtrar por estado");

    private final ComboBox<Asignacion.Tipo> filtroTipo =
            new ComboBox<>("Filtrar por tipo");

    private final VerticalLayout formulario =
            new VerticalLayout();

    public AsignacionView(
            AsignacionService asignacionService,
            AsignacionRepository asignacionRepository,
            UsuarioRepository usuarioRepository,
            CursoRepository cursoRepository,
            EvaluacionRepository evaluacionRepository) {

        this.asignacionService = asignacionService;
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.evaluacionRepository = evaluacionRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        crearEncabezado();
        crearFormulario();
        crearFiltros();
        configurarTabla();

        add(
                crearEncabezado(),
                formulario,
                crearFiltros(),
                tabla
        );

        actualizarTabla();
    }

    // ENCABEZADO

    private HorizontalLayout crearEncabezado() {

        H2 titulo = new H2("Gestión de asignaciones");

        Span descripcion = new Span(
                "Administra los cursos y evaluaciones asignados a los empleados."
        );

        descripcion.getStyle()
                .set("color", "#64748B")
                .set("font-size", "14px");

        VerticalLayout informacion =
                new VerticalLayout(titulo, descripcion);

        informacion.setPadding(false);
        informacion.setSpacing(false);

        Button actualizar = new Button(
                "Actualizar",
                event -> actualizarTabla()
        );

        actualizar.getStyle()
                .set("background", "#1769AA")
                .set("color", "#FFFFFF");

        HorizontalLayout encabezado =
                new HorizontalLayout(informacion, actualizar);

        encabezado.setWidthFull();
        encabezado.setAlignItems(Alignment.CENTER);
        encabezado.expand(informacion);

        return encabezado;
    }

    // FORMULARIO DE ASIGNACION

    private void crearFormulario() {

        H2 titulo = new H2("Nueva asignación");

        titulo.getStyle()
                .set("font-size", "20px")
                .set("color", "#1769AA");

        empleado.setItems(
                usuarioRepository.findAll().stream()
                        .filter(usuario ->
                                "empleado".equalsIgnoreCase(usuario.getRol())
                                && "activo".equalsIgnoreCase(usuario.getEstado())
                        )
                        .collect(Collectors.toList())
        );

        empleado.setItemLabelGenerator(usuario ->
                usuario.getNombres() + " " + usuario.getApellidos()
        );

        empleado.setPlaceholder("Selecciona un empleado");
        empleado.setWidthFull();

        tipo.setItems(Asignacion.Tipo.values());
        tipo.setPlaceholder("Selecciona el tipo");
        tipo.setWidthFull();

        curso.setItems(
                cursoRepository.findAll().stream()
                        .filter(item ->
                                item.getEstado() == Curso.EstadoCurso.activo
                        )
                        .collect(Collectors.toList())
        );

        curso.setItemLabelGenerator(Curso::getTitulo);
        curso.setPlaceholder("Selecciona un curso");
        curso.setWidthFull();

        evaluacion.setItems(
                evaluacionRepository.findAll().stream()
                        .filter(item ->
                                item.getEstado()
                                        == Evaluacion.EstadoEvaluacion.activo
                        )
                        .collect(Collectors.toList())
        );

        evaluacion.setItemLabelGenerator(Evaluacion::getTitulo);
        evaluacion.setPlaceholder("Selecciona una evaluación");
        evaluacion.setWidthFull();

        curso.setVisible(false);
        evaluacion.setVisible(false);

        tipo.addValueChangeListener(event -> {

            Asignacion.Tipo seleccionado = event.getValue();

            curso.clear();
            evaluacion.clear();

            curso.setVisible(
                    seleccionado == Asignacion.Tipo.curso
            );

            evaluacion.setVisible(
                    seleccionado == Asignacion.Tipo.evaluacion
            );
        });

        Button guardar = new Button(
                "Guardar asignación",
                event -> guardarAsignacion()
        );

        guardar.getStyle()
                .set("background", "#1769AA")
                .set("color", "#FFFFFF");

        HorizontalLayout campos = new HorizontalLayout(
                empleado,
                tipo,
                curso,
                evaluacion
        );

        campos.setWidthFull();
        campos.setAlignItems(Alignment.END);

        campos.setFlexGrow(1, empleado);
        campos.setFlexGrow(1, tipo);
        campos.setFlexGrow(1, curso);
        campos.setFlexGrow(1, evaluacion);

        formulario.setPadding(true);
        formulario.setSpacing(true);
        formulario.setWidthFull();

        formulario.add(
                titulo,
                campos,
                guardar
        );

        formulario.getStyle()
                .set("background", "#FFFFFF")
                .set("border", "1px solid #E2E8F0")
                .set("border-radius", "10px")
                .set("padding", "20px");

        // Mostrar formulario únicamente a administradores.

        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        boolean puedeGestionar = false;

        if (autenticacion != null) {

            puedeGestionar = autenticacion.getAuthorities().stream()
                    .anyMatch(authority -> {

                        String autoridad = authority.getAuthority();

                        return "ADMIN".equalsIgnoreCase(autoridad)
                                || "ROLE_ADMIN".equalsIgnoreCase(autoridad)
                                || "ADMIN_EMPRESA".equalsIgnoreCase(autoridad)
                                || "ROLE_ADMIN_EMPRESA".equalsIgnoreCase(autoridad);
                    });
        }

        formulario.setVisible(puedeGestionar);
    }

    // GUARDAR ASIGNACION

    private void guardarAsignacion() {

        if (empleado.isEmpty()
                || tipo.isEmpty()) {

            mostrarNotificacion(
                    "Debes seleccionar un empleado y un tipo de asignación.",
                    true
            );

            return;
        }

        Integer usuarioId = empleado.getValue().getId();

        Integer cursoId = null;
        Integer evaluacionId = null;

        if (tipo.getValue() == Asignacion.Tipo.curso) {

            if (curso.isEmpty()) {
                mostrarNotificacion(
                        "Debes seleccionar un curso.",
                        true
                );
                return;
            }

            cursoId = curso.getValue().getId();
        }

        if (tipo.getValue() == Asignacion.Tipo.evaluacion) {

            if (evaluacion.isEmpty()) {
                mostrarNotificacion(
                        "Debes seleccionar una evaluación.",
                        true
                );
                return;
            }

            evaluacionId = evaluacion.getValue().getId();
        }

        try {

            asignacionService.crearAsignacion(
                    usuarioId,
                    cursoId,
                    evaluacionId,
                    tipo.getValue()
            );

            mostrarNotificacion(
                    "La asignación se creó correctamente.",
                    false
            );

            empleado.clear();
            tipo.clear();
            curso.clear();
            evaluacion.clear();

            curso.setVisible(false);
            evaluacion.setVisible(false);

            actualizarTabla();

        } catch (Exception e) {

            mostrarNotificacion(
                    "No se pudo crear la asignación: " + e.getMessage(),
                    true
            );
        }
    }

    // FILTROS

    private HorizontalLayout crearFiltros() {

        filtroEstado.setItems(Asignacion.Estado.values());
        filtroEstado.setPlaceholder("Todos los estados");
        filtroEstado.setClearButtonVisible(true);

        filtroTipo.setItems(Asignacion.Tipo.values());
        filtroTipo.setPlaceholder("Todos los tipos");
        filtroTipo.setClearButtonVisible(true);

        Button buscar = new Button(
                "Aplicar filtros",
                event -> aplicarFiltros()
        );

        Button limpiar = new Button(
                "Limpiar",
                event -> {

                    filtroEstado.clear();
                    filtroTipo.clear();

                    actualizarTabla();
                }
        );

        filtroEstado.addValueChangeListener(
                event -> aplicarFiltros()
        );

        filtroTipo.addValueChangeListener(
                event -> aplicarFiltros()
        );

        HorizontalLayout filtros = new HorizontalLayout(
                filtroEstado,
                filtroTipo,
                buscar,
                limpiar
        );

        filtros.setAlignItems(Alignment.END);

        return filtros;
    }

    // CONFIGURAR TABLA

    private void configurarTabla() {

        tabla.addColumn(Asignacion::getId)
                .setHeader("ID")
                .setAutoWidth(true);

        tabla.addColumn(asignacion -> {

            Usuario usuario = asignacion.getUsuario();

            return usuario != null
                    ? usuario.getNombres() + " " + usuario.getApellidos()
                    : "Sin usuario";

        }).setHeader("Empleado");

        tabla.addColumn(asignacion -> {

            if (asignacion.getTipo() == Asignacion.Tipo.curso) {

                return asignacion.getCurso() != null
                        ? asignacion.getCurso().getTitulo()
                        : "Curso no disponible";
            }

            if (asignacion.getTipo() == Asignacion.Tipo.evaluacion) {

                return asignacion.getEvaluacion() != null
                        ? asignacion.getEvaluacion().getTitulo()
                        : "Evaluación no disponible";
            }

            return "Sin contenido";

        }).setHeader("Contenido asignado");

        tabla.addColumn(asignacion ->
                asignacion.getTipo() != null
                        ? asignacion.getTipo().name()
                        : "Sin tipo"
        ).setHeader("Tipo");

        tabla.addColumn(asignacion ->
                asignacion.getFechaAsignacion() != null
                        ? asignacion.getFechaAsignacion().format(
                                DateTimeFormatter.ofPattern(
                                        "dd/MM/yyyy HH:mm"
                                )
                        )
                        : "Sin fecha"
        ).setHeader("Fecha de asignación");

        tabla.addColumn(asignacion ->
                asignacion.getEstado() != null
                        ? asignacion.getEstado().name()
                        : "Sin estado"
        ).setHeader("Estado");

        tabla.setSizeFull();
        tabla.setAllRowsVisible(true);
    }

    // ACTUALIZAR TABLA

    private void actualizarTabla() {

        try {

            List<Asignacion> asignaciones =
                    asignacionService.listarAsignaciones();

            tabla.setItems(asignaciones);

        } catch (Exception e) {

            tabla.setItems(new ArrayList<>());

            mostrarNotificacion(
                    "No fue posible cargar las asignaciones: "
                            + e.getMessage(),
                    true
            );
        }
    }

    // APLICAR FILTROS

    private void aplicarFiltros() {

        try {

            List<Asignacion> asignaciones =
                    new ArrayList<>(
                            asignacionService.listarAsignaciones()
                    );

            if (filtroEstado.getValue() != null) {

                asignaciones.removeIf(asignacion ->
                        asignacion.getEstado()
                                != filtroEstado.getValue()
                );
            }

            if (filtroTipo.getValue() != null) {

                asignaciones.removeIf(asignacion ->
                        asignacion.getTipo()
                                != filtroTipo.getValue()
                );
            }

            tabla.setItems(asignaciones);

        } catch (Exception e) {

            mostrarNotificacion(
                    "No se pudieron aplicar los filtros.",
                    true
            );
        }
    }

    // NOTIFICACIONES

    private void mostrarNotificacion(
            String mensaje,
            boolean error) {

        Notification notificacion =
                Notification.show(mensaje, 4000,
                        Notification.Position.MIDDLE);

        if (error) {
            notificacion.addThemeVariants(
                    NotificationVariant.LUMO_ERROR
            );
        } else {
            notificacion.addThemeVariants(
                    NotificationVariant.LUMO_SUCCESS
            );
        }
    }
}


private void cargarEmpleados() {

    Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
        empleado.setItems(new ArrayList<>());
        return;
    }

    Usuario usuarioActual = usuarioRepository
            .findByCorreo(authentication.getName())
            .orElse(null);

    if (usuarioActual == null) {
        empleado.setItems(new ArrayList<>());
        return;
    }

    List<Usuario> empleados;

    if ("admin".equalsIgnoreCase(usuarioActual.getRol())) {

        empleados = usuarioRepository.findAll();

    } else if ("admin_empresa".equalsIgnoreCase(usuarioActual.getRol())
            && usuarioActual.getEmpresa() != null) {

        empleados = usuarioRepository.findByEmpresa_Id(
                usuarioActual.getEmpresa().getId()
        );

    } else {

        empleados = new ArrayList<>();
    }

    empleados.removeIf(usuario ->
            !"empleado".equalsIgnoreCase(usuario.getRol())
            || !"activo".equalsIgnoreCase(usuario.getEstado())
    );

    empleado.setItems(empleados);
}
