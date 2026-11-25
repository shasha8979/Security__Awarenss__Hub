
package com.sah.views;

import com.sah.entity.Usuario;
import com.sah.repository.UsuarioRepository;
import com.sah.repository.EmpresaRepository;
import com.sah.repository.CursoRepository;
import com.sah.repository.EvaluacionRepository;
import com.sah.repository.AsignacionRepository;
import com.sah.entity.Asignacion;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import jakarta.annotation.security.RolesAllowed;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | Security Awareness Hub")
@RolesAllowed({"ADMIN", "ADMIN_EMPRESA", "EMPLEADO"})
public class MainView extends VerticalLayout {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final CursoRepository cursoRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final AsignacionRepository asignacionRepository;

public MainView(
        UsuarioRepository usuarioRepository,
        EmpresaRepository empresaRepository,
        CursoRepository cursoRepository,
        EvaluacionRepository evaluacionRepository) {

    this.usuarioRepository = usuarioRepository;
    this.empresaRepository = empresaRepository;
    this.cursoRepository = cursoRepository;
    this.evaluacionRepository = evaluacionRepository;
    this.asignacionRepository = asignacionRepository;

    setSizeFull();
    setPadding(true);
    setSpacing(true);
    setDefaultHorizontalComponentAlignment(
            Alignment.CENTER
    );

    construirDashboard();
}
    private void construirDashboard() {

        Usuario usuario = obtenerUsuarioActual();

        if (usuario == null) {
            add(new H1("No se pudo identificar al usuario."));
            return;
        }

        String rol = usuario.getRol().toLowerCase();

        VerticalLayout contenido = new VerticalLayout();
        contenido.setWidthFull();
        contenido.setMaxWidth("1200px");
        contenido.setPadding(true);
        contenido.setSpacing(true);

        contenido.add(
                new H1("Bienvenido al Security Awareness Hub"),
                new Paragraph(
                        "Hola, " + usuario.getNombres()
                                + ". Este es tu espacio de capacitación "
                                + "en ciberseguridad."
                )
        );

        switch (rol) {

            case "admin":
                construirPanelAdministrador(contenido);
                break;

            case "admin_empresa":
                construirPanelEmpresa(contenido);
                break;

            case "empleado":
                construirPanelEstudiante(contenido);
                break;

            default:
                contenido.add(
                        new Paragraph(
                                "Tu perfil no tiene un panel asignado."
                        )
                );
                break;
        }

        add(contenido);
    }

    private void construirPanelAdministrador(
            VerticalLayout contenido) {
                construirEstadisticasAdministrador(contenido);        
        contenido.add(
                new H2("Panel de administración"),
                new Paragraph(
                        "Gestiona los recursos y las actividades "
                                + "generales de la plataforma."
                )
        );

        HorizontalLayout tarjetas = new HorizontalLayout();
        tarjetas.setWidthFull();
        tarjetas.setFlexWrap(true);

        tarjetas.add(
                crearTarjeta(
                        "Usuarios",
                        "Administra las cuentas y los perfiles.",
                        UsuarioView.class
                ),
                crearTarjeta(
                        "Empresas",
                        "Gestiona las organizaciones registradas.",
                        EmpresaView.class
                ),
                crearTarjeta(
                        "Evaluaciones",
                        "Administra las evaluaciones de capacitación.",
                        EvaluacionView.class
                ),
                crearTarjeta(
                        "Asignaciones",
                        "Organiza los contenidos asignados.",
                        AsignacionView.class
                )
        );

        contenido.add(tarjetas);
    }

    private void construirPanelEmpresa(
            VerticalLayout contenido) {

        contenido.add(
                new H2("Panel de capacitación empresarial"),
                new Paragraph(
                        "Desde aquí puedes organizar y supervisar "
                                + "las capacitaciones de tu empresa."
                )
        );

        contenido.add(
                crearTarjeta(
                        "Asignaciones",
                        "Consulta y organiza las capacitaciones "
                                + "de los estudiantes de tu empresa.",
                        AsignacionView.class
                )
        );
    }

    private void construirPanelEstudiante(
            VerticalLayout contenido) {
                construirEstadisticasEstudiante(
                        contenido,
                        obtenerUsuarioActual()
                ); 
        contenido.add(
                new H2("Mi espacio de aprendizaje"),
                new Paragraph(
                        "Aquí puedes consultar tus capacitaciones "
                                + "y realizar las actividades asignadas."
                )
        );

        contenido.add(
                crearTarjeta(
                        "Mis capacitaciones",
                        "Consulta los cursos y evaluaciones "
                                + "que te han asignado.",
                                MisCapacitacionesView.class
                )
        );
    }

    private Div crearTarjeta(
            String titulo,
            String descripcion,
            Class<? extends Component> destino) {

        Div tarjeta = new Div();

        tarjeta.setWidth("260px");
        tarjeta.getStyle()
                .set("padding", "20px")
                .set("border-radius", "12px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.06)");

        H2 encabezado = new H2(titulo);
        encabezado.getStyle().set("font-size", "18px");

        Paragraph texto = new Paragraph(descripcion);

        RouterLink enlace = new RouterLink(
                "Acceder",
                destino
        );

        tarjeta.add(encabezado, texto, enlace);

        return tarjeta;
    }

    private Usuario obtenerUsuarioActual() {

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
}



private void construirEstadisticasAdministrador(
        VerticalLayout contenido) {

    HorizontalLayout estadisticas = new HorizontalLayout();
    estadisticas.setWidthFull();
    estadisticas.setFlexWrap(true);

    estadisticas.add(
            crearIndicador(
                    "Empresas registradas",
                    empresaRepository.count(),
                    "building-2"
            ),
            crearIndicador(
                    "Usuarios registrados",
                    usuarioRepository.count(),
                    "users"
            ),
            crearIndicador(
                    "Cursos registrados",
                    cursoRepository.count(),
                    "book-open"
            ),
            crearIndicador(
                    "Evaluaciones registradas",
                    evaluacionRepository.count(),
                    "clipboard-check"
            )
    );

    contenido.add(estadisticas);
}


private void construirEstadisticasEmpresa(
        VerticalLayout contenido,
        Usuario administrador) {

    if (administrador.getEmpresa() == null) {
        contenido.add(
                new Paragraph(
                        "Tu cuenta no tiene una empresa asociada."
                )
        );
        return;
    }

    Integer empresaId = administrador.getEmpresa().getId();

    long totalEstudiantes = usuarioRepository
            .findByEmpresa_Id(empresaId)
            .stream()
            .filter(usuario ->
                    "empleado".equalsIgnoreCase(usuario.getRol())
                    && "activo".equalsIgnoreCase(usuario.getEstado())
            )
            .count();

    var asignaciones = asignacionRepository
            .findByUsuario_Empresa_Id(empresaId);

    long totalAsignaciones = asignaciones.size();

    long pendientes = asignaciones.stream()
            .filter(asignacion ->
                    asignacion.getEstado() == Asignacion.Estado.pendiente)
            .count();

    long completadas = asignaciones.stream()
            .filter(asignacion ->
                    asignacion.getEstado() == Asignacion.Estado.completado)
            .count();

    HorizontalLayout estadisticas = new HorizontalLayout();
    estadisticas.setWidthFull();
    estadisticas.setFlexWrap(true);

    estadisticas.add(
            crearIndicador(
                    "Estudiantes activos",
                    totalEstudiantes,
                    "users"
            ),
            crearIndicador(
                    "Capacitaciones asignadas",
                    totalAsignaciones,
                    "book-open"
            ),
            crearIndicador(
                    "Pendientes",
                    pendientes,
                    "clock"
            ),
            crearIndicador(
                    "Completadas",
                    completadas,
                    "circle-check"
            )
    );

    contenido.add(estadisticas);
}



private void construirEstadisticasEstudiante(
        VerticalLayout contenido,
        Usuario estudiante) {

    if (estudiante == null) {
        contenido.add(
                new Paragraph("No se pudo identificar al estudiante.")
        );
        return;
    }

    var asignaciones = asignacionRepository
            .findByUsuario_Id(estudiante.getId());

    long total = asignaciones.size();

    long pendientes = asignaciones.stream()
            .filter(asignacion ->
                    asignacion.getEstado() == Asignacion.Estado.pendiente)
            .count();

    long enProgreso = asignaciones.stream()
            .filter(asignacion ->
                    asignacion.getEstado() == Asignacion.Estado.progreso)
            .count();

    long completadas = asignaciones.stream()
            .filter(asignacion ->
                    asignacion.getEstado() == Asignacion.Estado.completado)
            .count();

    HorizontalLayout estadisticas = new HorizontalLayout();
    estadisticas.setWidthFull();
    estadisticas.setFlexWrap(true);

    estadisticas.add(
            crearIndicador(
                    "Mis capacitaciones",
                    total,
                    "book-open"
            ),
            crearIndicador(
                    "Pendientes",
                    pendientes,
                    "clock"
            ),
            crearIndicador(
                    "En progreso",
                    enProgreso,
                    "activity"
            ),
            crearIndicador(
                    "Completadas",
                    completadas,
                    "circle-check"
            )
    );

    contenido.add(estadisticas);
}



private Div crearIndicador(
        String titulo,
        long cantidad,
        String icono) {

    Div tarjeta = new Div();

    tarjeta.setWidth("220px");
    tarjeta.getStyle()
            .set("padding", "20px")
            .set("border-radius", "12px")
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.06)");

    Span nombre = new Span(titulo);
    nombre.getStyle()
            .set("font-size", "14px")
            .set("color", "var(--lumo-secondary-text-color)");

    H1 valor = new H1(String.valueOf(cantidad));
    valor.getStyle()
            .set("font-size", "30px")
            .set("margin", "12px 0 0 0");

    tarjeta.add(nombre, valor);

    return tarjeta;
}
