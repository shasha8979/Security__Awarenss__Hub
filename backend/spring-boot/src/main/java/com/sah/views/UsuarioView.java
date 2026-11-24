
package com.sah.views;

import com.sah.entity.Empresa;
import com.sah.entity.Usuario;
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

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "usuarios", layout = MainLayout.class)
@PageTitle("Gestión de usuarios | SAH")
@RolesAllowed("ADMIN")
public class UsuarioView extends VerticalLayout {

    private final UsuarioService usuarioService;

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

    public UsuarioView(UsuarioService usuarioService) {

        this.usuarioService = usuarioService;

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
                new H2("Usuarios registrados"),
                grid
        );

        actualizarTabla();
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
