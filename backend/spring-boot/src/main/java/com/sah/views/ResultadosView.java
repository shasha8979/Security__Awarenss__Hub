package com.sah.views;

import com.sah.model.Evaluacion;
import com.sah.model.Resultado;
import com.sah.model.ResultadoRespuesta;
import com.sah.model.Usuario;
import com.sah.repository.UsuarioRepository;
import com.sah.service.EvaluacionService;
import com.sah.service.ResultadoService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consulta de resultados de las evaluaciones presentadas por los usuarios.
 */
@Route(value = "resultados", layout = MainLayout.class)
@PageTitle("Resultados | Security Awareness Hub")
@RolesAllowed("ADMIN")
public class ResultadosView extends VerticalLayout {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ResultadoService resultadoService;
    private final EvaluacionService evaluacionService;
    private final UsuarioRepository usuarioRepository;

    private final ComboBox<Evaluacion> filtroEvaluacion = new ComboBox<>("Evaluación");
    private final Checkbox soloMejorIntento = new Checkbox("Mostrar solo el mejor intento de cada usuario");
    private final Grid<Resultado> grid = new Grid<>(Resultado.class, false);

    private Map<Integer, Usuario> usuariosPorId = Map.of();
    private Map<Integer, Evaluacion> evaluacionesPorId = Map.of();

    public ResultadosView(
            ResultadoService resultadoService,
            EvaluacionService evaluacionService,
            UsuarioRepository usuarioRepository) {

        this.resultadoService = resultadoService;
        this.evaluacionService = evaluacionService;
        this.usuarioRepository = usuarioRepository;

        crearFiltros();
        configurarTabla();
        actualizarTabla();

        Paragraph ayuda = new Paragraph(
                "Cada fila es un intento. Un usuario puede presentar varias veces la misma "
                        + "evaluación; para estadísticas y reportes cuenta su mejor intento.");
        ayuda.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "0");

        HorizontalLayout filtros = new HorizontalLayout(filtroEvaluacion, soloMejorIntento);
        filtros.setAlignItems(FlexComponent.Alignment.BASELINE);
        filtros.getStyle().set("flex-wrap", "wrap");

        VerticalLayout contenido = new VerticalLayout(
                new H1("Resultados de evaluaciones"),
                ayuda,
                filtros,
                grid
        );

        contenido.setSizeFull();
        contenido.setPadding(true);
        contenido.setSpacing(true);

        setSizeFull();
        setPadding(false);
        add(contenido);
    }

    private void crearFiltros() {

        List<Evaluacion> evaluaciones = evaluacionService.listarEvaluaciones(null);
        evaluacionesPorId = evaluaciones.stream()
                .collect(Collectors.toMap(Evaluacion::getId, Function.identity()));

        filtroEvaluacion.setItems(evaluaciones);
        filtroEvaluacion.setItemLabelGenerator(Evaluacion::getTitulo);
        filtroEvaluacion.setPlaceholder("Todas las evaluaciones");
        filtroEvaluacion.setClearButtonVisible(true);
        filtroEvaluacion.setWidth("320px");
        filtroEvaluacion.addValueChangeListener(event -> actualizarTabla());

        soloMejorIntento.addValueChangeListener(event -> actualizarTabla());
    }

    private void configurarTabla() {

        grid.addColumn(r -> nombreUsuario(r.getUsuarioId()))
                .setHeader("Usuario")
                .setFlexGrow(2);

        grid.addColumn(r -> {
                    Usuario u = usuariosPorId.get(r.getUsuarioId());
                    return u == null ? "" : u.getCorreo();
                })
                .setHeader("Correo")
                .setFlexGrow(2);

        grid.addColumn(r -> {
                    Evaluacion e = evaluacionesPorId.get(r.getEvaluacionId());
                    return e == null ? "Evaluación no encontrada" : e.getTitulo();
                })
                .setHeader("Evaluación")
                .setFlexGrow(2);

        grid.addColumn(Resultado::getIntento)
                .setHeader("Intento")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(r -> r.getRespuestasCorrectas() + " / " + r.getTotalPreguntas())
                .setHeader("Correctas")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(r -> r.getPorcentaje().stripTrailingZeros().toPlainString() + " %")
                .setHeader("Nota")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(r -> r.getPorcentajeRequerido() + " %")
                .setHeader("Requerido")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(this::crearEtiquetaAprobado)
                .setHeader("Estado")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(r -> r.getFechaPresentacion() == null
                        ? ""
                        : r.getFechaPresentacion().format(FORMATO_FECHA))
                .setHeader("Fecha")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(r -> {
                    Button detalle = new Button("Detalle", VaadinIcon.EYE.create());
                    detalle.addThemeVariants(ButtonVariant.LUMO_SMALL);
                    detalle.addClickListener(event -> abrirDetalle(r));
                    return detalle;
                })
                .setHeader("Acciones")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.setWidthFull();
        grid.setEmptyStateText("Todavía no hay resultados registrados.");
    }

    private void actualizarTabla() {

        Evaluacion evaluacion = filtroEvaluacion.getValue();

        List<Resultado> resultados =
                resultadoService.listar(evaluacion == null ? null : evaluacion.getId());

        if (soloMejorIntento.getValue()) {
            resultados = ResultadoService.mejoresIntentos(resultados);
        }

        List<Integer> idsUsuarios = resultados.stream()
                .map(Resultado::getUsuarioId)
                .distinct()
                .toList();

        usuariosPorId = usuarioRepository.findAllById(idsUsuarios).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));

        grid.setItems(resultados);
    }

    private String nombreUsuario(Integer usuarioId) {
        Usuario u = usuariosPorId.get(usuarioId);
        return u == null ? "Usuario no encontrado" : u.getNombres() + " " + u.getApellidos();
    }

    private Span crearEtiquetaAprobado(Resultado resultado) {

        boolean aprobado = resultado.isAprobado();

        Span estado = new Span(aprobado ? "Aprobado" : "Reprobado");
        estado.getElement().getThemeList().add(aprobado ? "badge success" : "badge error");
        return estado;
    }

    private void abrirDetalle(Resultado fila) {

        Resultado resultado;

        try {
            resultado = resultadoService.buscarConRespuestas(fila.getId());
        } catch (IllegalArgumentException e) {
            Notification.show(e.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Grid<ResultadoRespuesta> respuestas = new Grid<>(ResultadoRespuesta.class, false);

        respuestas.addColumn(ResultadoRespuesta::getEnunciadoPregunta)
                .setHeader("Pregunta")
                .setFlexGrow(3);

        respuestas.addColumn(r -> r.getTextoOpcion() == null ? "Sin responder" : r.getTextoOpcion())
                .setHeader("Respuesta elegida")
                .setFlexGrow(2);

        respuestas.addColumn(r -> r.isEsCorrecta() ? "✔ Correcta" : "✘ Incorrecta")
                .setHeader("Resultado")
                .setAutoWidth(true)
                .setFlexGrow(0);

        respuestas.setItems(resultado.getRespuestas());
        respuestas.setAllRowsVisible(true);
        respuestas.setEmptyStateText("Este intento no tiene respuestas registradas.");

        Evaluacion evaluacion = evaluacionesPorId.get(resultado.getEvaluacionId());

        Dialog dialogo = new Dialog();
        dialogo.setHeaderTitle(nombreUsuario(resultado.getUsuarioId()) + " · "
                + (evaluacion == null ? "Evaluación" : evaluacion.getTitulo())
                + " · Intento " + resultado.getIntento());
        dialogo.setWidth("800px");
        dialogo.add(
                new Paragraph("Nota: " + resultado.getPorcentaje().stripTrailingZeros().toPlainString()
                        + " % (requerido " + resultado.getPorcentajeRequerido() + " %) · "
                        + (resultado.isAprobado() ? "Aprobado" : "Reprobado")),
                respuestas);

        Button cerrar = new Button("Cerrar", event -> dialogo.close());
        dialogo.getFooter().add(cerrar);

        dialogo.open();
    }
}
