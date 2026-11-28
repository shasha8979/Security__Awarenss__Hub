
package com.sah.views;

import com.sah.dto.ErrorFilaDTO;
import com.sah.dto.ResultadoCargaDTO;
import com.sah.model.Empresa;
import com.sah.model.Usuario;
import com.sah.service.CargaUsuariosCsvService;
import com.sah.service.UsuarioService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.server.streams.UploadHandler;

import java.io.ByteArrayInputStream;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "usuarios", layout = MainLayout.class)
@PageTitle("Gestión de usuarios | SAH")
@RolesAllowed("ADMIN")
public class UsuarioView extends VerticalLayout {

    private final UsuarioService usuarioService;
    private final CargaUsuariosCsvService cargaUsuariosCsvService;

    private final Grid<ErrorFilaDTO> erroresCarga = new Grid<>(ErrorFilaDTO.class, false);

    private final TextField nombres = new TextField("Nombres");
    private final TextField apellidos = new TextField("Apellidos");
    private final EmailField correo = new EmailField("Correo electrónico");
    private final PasswordField password = new PasswordField("Contraseña");

    private final Select<String> rol = new Select<>();
    private final Select<String> estado = new Select<>();

    private final ComboBox<Empresa> empresa = new ComboBox<>("Empresa");

    private final Grid<Usuario> grid = new Grid<>(Usuario.class, false);

    private final Button guardar = new Button("Registrar usuario");
    private final Button cancelar = new Button("Limpiar formulario");

    private Integer usuarioId = null;

    public UsuarioView(
            UsuarioService usuarioService,
            CargaUsuariosCsvService cargaUsuariosCsvService) {

        this.usuarioService = usuarioService;
        this.cargaUsuariosCsvService = cargaUsuariosCsvService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configurarFormulario();
        configurarTabla();

        H2 titulo = new H2("Gestión de usuarios");

        Paragraph descripcion = new Paragraph(
                "Administra los usuarios, sus roles, empresas y estados."
        );

        FormLayout formulario = new FormLayout();

        formulario.add(
                nombres,
                apellidos,
                correo,
                password,
                rol,
                empresa,
                estado
        );

        formulario.setMaxWidth("850px");

        HorizontalLayout botones = new HorizontalLayout(
                guardar,
                cancelar
        );

        botones.setSpacing(true);

        VerticalLayout panelFormulario = new VerticalLayout(
                new H2("Información del usuario"),
                formulario,
                botones
        );

        panelFormulario.setPadding(true);
        panelFormulario.setSpacing(true);
        panelFormulario.setWidthFull();

        add(
                titulo,
                descripcion,
                panelFormulario,
                crearPanelCargaCsv(),
                new H2("Usuarios registrados"),
                grid
        );

        actualizarTabla();
    }

    /**
     * Carga masiva por CSV (Sesion 8). Usa el mismo servicio que el
     * endpoint POST /api/usuarios/carga-csv, así ambas vías aplican
     * exactamente las mismas reglas.
     */
    private VerticalLayout crearPanelCargaCsv() {

        Paragraph formato = new Paragraph(
                "Archivo .csv en UTF-8 (máximo "
                        + CargaUsuariosCsvService.MAXIMO_FILAS
                        + " filas, 2 MB) con la cabecera: "
                        + "nombres;apellidos;correo;nit_empresa;rol. "
                        + "El rol es opcional (empleado o admin_empresa). "
                        + "Cada usuario recibe por correo una contraseña temporal."
        );

        Span resumen = new Span();
        resumen.setVisible(false);

        erroresCarga.addColumn(ErrorFilaDTO::fila).setHeader("Línea").setAutoWidth(true);
        erroresCarga.addColumn(ErrorFilaDTO::correo).setHeader("Correo").setAutoWidth(true);
        erroresCarga.addColumn(ErrorFilaDTO::mensaje).setHeader("Motivo").setFlexGrow(1);
        erroresCarga.setAllRowsVisible(true);
        erroresCarga.setVisible(false);

        Upload upload = new Upload(UploadHandler.inMemory((metadata, datos) -> {
            try {
                ResultadoCargaDTO resultado = cargaUsuariosCsvService.cargar(
                        metadata.fileName(), new ByteArrayInputStream(datos));

                resumen.setText("Filas leídas: " + resultado.totalFilas()
                        + " · Registrados: " + resultado.creados()
                        + " · Rechazados: " + resultado.rechazados());
                resumen.setVisible(true);

                erroresCarga.setItems(resultado.errores());
                erroresCarga.setVisible(!resultado.errores().isEmpty());

                Notification.show(resultado.creados() + " usuarios registrados.");
                actualizarTabla();

            } catch (IllegalArgumentException ex) {
                resumen.setVisible(false);
                erroresCarga.setVisible(false);
                Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        }));

        upload.setAcceptedFileExtensions(".csv");
        upload.setI18n(textosEnEspanol());
        upload.setMaxFiles(1);
        upload.setMaxFileSize(2 * 1024 * 1024);
        upload.addFileRejectedListener(evento ->
                Notification.show(evento.getErrorMessage(), 5000,
                        Notification.Position.MIDDLE));

        VerticalLayout panel = new VerticalLayout(
                new H2("Carga masiva de usuarios (CSV)"),
                formato,
                upload,
                resumen,
                erroresCarga
        );

        panel.setPadding(true);
        panel.setSpacing(true);
        panel.setWidthFull();

        return panel;
    }

    /** El componente Upload trae sus textos en inglés; se traducen. */
    private static UploadI18N textosEnEspanol() {

        UploadI18N i18n = new UploadI18N();

        i18n.setAddFiles(new UploadI18N.AddFiles()
                .setOne("Seleccionar archivo CSV...")
                .setMany("Seleccionar archivos..."));

        i18n.setDropFiles(new UploadI18N.DropFiles()
                .setOne("o arrástralo aquí")
                .setMany("o arrástralos aquí"));

        i18n.setError(new UploadI18N.Error()
                .setTooManyFiles("Solo puedes subir un archivo a la vez.")
                .setFileIsTooBig("El archivo supera el máximo de 2 MB.")
                .setIncorrectFileType("El archivo debe tener extensión .csv."));

        i18n.setUploading(new UploadI18N.Uploading()
                .setStatus(new UploadI18N.Uploading.Status()
                        .setConnecting("Conectando...")
                        .setStalled("Detenido")
                        .setProcessing("Procesando archivo...")
                        .setHeld("En cola"))
                .setRemainingTime(new UploadI18N.Uploading.RemainingTime()
                        .setPrefix("tiempo restante: ")
                        .setUnknown("calculando tiempo restante"))
                .setError(new UploadI18N.Uploading.Error()
                        .setServerUnavailable("El servidor no responde.")
                        .setUnexpectedServerError("Error inesperado del servidor.")
                        .setForbidden("No tienes permiso para subir archivos.")));

        i18n.setUnits(List.of("B", "kB", "MB", "GB", "TB"));

        return i18n;
    }

    private void configurarFormulario() {

        nombres.setRequired(true);
        apellidos.setRequired(true);
        correo.setRequired(true);
        password.setRequiredIndicatorVisible(true);

        nombres.setMaxLength(100);
        apellidos.setMaxLength(100);
        correo.setMaxLength(150);

        password.setMinLength(8);
        password.setHelperText(
                "Mínimo 8 caracteres. Déjala vacía al editar si no deseas cambiarla."
        );

        rol.setLabel("Rol");
        rol.setItems(
                "admin",
                "admin_empresa",
                "empleado"
        );
        rol.setItemLabelGenerator(valor -> {
            switch (valor) {
                case "admin":
                    return "Administrador general";
                case "admin_empresa":
                    return "Administrador de empresa";
                default:
                    return "Empleado";
            }
        });
        rol.setValue("empleado");

        estado.setLabel("Estado");
        estado.setItems("activo", "inactivo");
        estado.setItemLabelGenerator(valor ->
                valor.equals("activo") ? "Activo" : "Inactivo"
        );
        estado.setValue("activo");

        empresa.setItems(usuarioService.listarEmpresasActivas());
        empresa.setItemLabelGenerator(Empresa::getNombre);
        empresa.setClearButtonVisible(true);
        empresa.setPlaceholder("Seleccionar empresa");

        rol.addValueChangeListener(event -> actualizarCampoEmpresa());

        guardar.addClickListener(event -> guardarUsuario());

        cancelar.addClickListener(event -> limpiarFormulario());

        actualizarCampoEmpresa();
    }

    private void actualizarCampoEmpresa() {

        boolean requiereEmpresa =
                rol.getValue() != null
                        && !rol.getValue().equals("admin");

        empresa.setEnabled(requiereEmpresa);
        empresa.setRequiredIndicatorVisible(requiereEmpresa);

        if (!requiereEmpresa) {
            empresa.clear();
        }
    }

    private void configurarTabla() {

        grid.addColumn(Usuario::getId)
                .setHeader("ID")
                .setAutoWidth(true);

        grid.addColumn(Usuario::getNombres)
                .setHeader("Nombres")
                .setAutoWidth(true);

        grid.addColumn(Usuario::getApellidos)
                .setHeader("Apellidos")
                .setAutoWidth(true);

        grid.addColumn(Usuario::getCorreo)
                .setHeader("Correo electrónico")
                .setAutoWidth(true);

        grid.addColumn(usuario ->
                usuario.getRol() != null
                        ? usuario.getRol()
                        : "Sin rol"
        ).setHeader("Rol").setAutoWidth(true);

        grid.addColumn(usuario ->
                usuario.getEmpresa() != null
                        ? usuario.getEmpresa().getNombre()
                        : "Sin empresa"
        ).setHeader("Empresa").setAutoWidth(true);

        grid.addColumn(Usuario::getEstado)
                .setHeader("Estado")
                .setAutoWidth(true);

        grid.addComponentColumn(usuario ->
                new Button("Editar", event ->
                        cargarUsuario(usuario)
                )
        ).setHeader("Acciones");

        grid.setSizeFull();
        grid.setPageSize(10);
    }

    private void guardarUsuario() {

        try {

            if (nombres.isEmpty()
                    || apellidos.isEmpty()
                    || correo.isEmpty()
                    || rol.isEmpty()
                    || estado.isEmpty()) {

                Notification.show(
                        "Completa todos los campos obligatorios."
                );
                return;
            }

            if (!correo.isInvalid()) {

                Usuario datos = new Usuario();

                datos.setNombres(nombres.getValue().trim());
                datos.setApellidos(apellidos.getValue().trim());
                datos.setCorreo(correo.getValue().trim());
                datos.setRol(rol.getValue());
                datos.setEstado(estado.getValue());

                Integer empresaId = empresa.getValue() != null
                        ? empresa.getValue().getId()
                        : null;

                if (usuarioId == null) {

                    if (password.isEmpty()) {
                        Notification.show(
                                "Debes ingresar una contraseña."
                        );
                        return;
                    }

                    usuarioService.registrarUsuario(
                            datos,
                            password.getValue(),
                            empresaId
                    );

                    Notification.show(
                            "Usuario registrado correctamente."
                    );

                } else {

                    usuarioService.actualizarUsuario(
                            usuarioId,
                            datos,
                            password.getValue(),
                            empresaId
                    );

                    Notification.show(
                            "Usuario actualizado correctamente."
                    );
                }

                actualizarTabla();
                limpiarFormulario();

            } else {

                Notification.show(
                        "Verifica el formato del correo electrónico."
                );
            }

        } catch (IllegalArgumentException e) {

            Notification.show(e.getMessage());

        } catch (Exception e) {

            Notification.show(
                    "No fue posible guardar el usuario. Revisa los datos."
            );
        }
    }

    private void cargarUsuario(Usuario usuario) {

        usuarioId = usuario.getId();

        nombres.setValue(usuario.getNombres());
        apellidos.setValue(usuario.getApellidos());
        correo.setValue(usuario.getCorreo());

        password.clear();
        password.setRequiredIndicatorVisible(false);

        rol.setValue(usuario.getRol());
        estado.setValue(usuario.getEstado());

        empresa.clear();

        if (usuario.getEmpresa() != null) {

            Integer empresaId = usuario.getEmpresa().getId();

            List<Empresa> empresas =
                    usuarioService.listarEmpresasActivas();

            empresas.stream()
                    .filter(item -> item.getId().equals(empresaId))
                    .findFirst()
                    .ifPresent(empresa::setValue);
        }

        guardar.setText("Guardar cambios");

        Notification.show(
                "Editando usuario: " + usuario.getCorreo()
        );
    }

    private void limpiarFormulario() {

        usuarioId = null;

        nombres.clear();
        apellidos.clear();
        correo.clear();
        password.clear();

        rol.setValue("empleado");
        estado.setValue("activo");
        empresa.clear();

        password.setRequiredIndicatorVisible(true);

        guardar.setText("Registrar usuario");

        actualizarCampoEmpresa();
    }

    private void actualizarTabla() {

        grid.setItems(usuarioService.listarUsuarios());
    }
}
