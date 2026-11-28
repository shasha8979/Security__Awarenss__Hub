
package com.sah.views;

import com.sah.model.Empresa;
import com.sah.repository.EmpresaRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "empresas", layout = MainLayout.class)
@PageTitle("Gestión de empresas | SAH")
@RolesAllowed("ADMIN")
public class EmpresaView extends VerticalLayout {

    private final EmpresaRepository empresaRepository;

    private final Grid<Empresa> grid = new Grid<>(Empresa.class, false);
    private final Binder<Empresa> binder = new Binder<>(Empresa.class);

    private final TextField nombre = new TextField("Nombre de la empresa");
    private final TextField nit = new TextField("NIT");
    private final EmailField correo = new EmailField("Correo electrónico");
    private final TextField telefono = new TextField("Teléfono");
    private final TextField direccion = new TextField("Dirección");

    private final ComboBox<Empresa.TipoEmpresa> tipo =
            new ComboBox<>("Tipo de empresa");

    private final ComboBox<Empresa.EstadoEmpresa> estado =
            new ComboBox<>("Estado");

    private Empresa empresaActual;

    public EmpresaView(EmpresaRepository empresaRepository) {

        this.empresaRepository = empresaRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configurarFormulario();
        configurarGrid();

        H2 titulo = new H2("Gestión de empresas");
        Paragraph descripcion = new Paragraph(
                "Administra las empresas registradas en Security Awareness Hub."
        );

        Button nuevaEmpresa = new Button(
                "Registrar empresa",
                event -> abrirFormulario(null)
        );

        HorizontalLayout encabezado =
                new HorizontalLayout(titulo, nuevaEmpresa);

        encabezado.setWidthFull();
        encabezado.setAlignItems(Alignment.CENTER);
        encabezado.expand(titulo);

        add(encabezado, descripcion, grid);

        actualizarGrid();
    }

    private void configurarFormulario() {

        binder.forField(nombre)
                .asRequired("El nombre es obligatorio")
                .bind(Empresa::getNombre, Empresa::setNombre);

        binder.forField(nit)
                .asRequired("El NIT es obligatorio")
                .bind(Empresa::getNit, Empresa::setNit);

        binder.forField(correo)
                .asRequired("El correo es obligatorio")
                .withValidator(
                        valor -> valor.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),
                        "Ingresa un correo válido"
                )
                .bind(Empresa::getCorreo, Empresa::setCorreo);

        binder.forField(telefono)
                .bind(Empresa::getTelefono, Empresa::setTelefono);

        binder.forField(direccion)
                .bind(Empresa::getDireccion, Empresa::setDireccion);

        tipo.setItems(Empresa.TipoEmpresa.values());
        tipo.setItemLabelGenerator(this::nombreTipo);

        binder.forField(tipo)
                .asRequired("Selecciona un tipo de empresa")
                .bind(Empresa::getTipo, Empresa::setTipo);

        estado.setItems(Empresa.EstadoEmpresa.values());
        estado.setItemLabelGenerator(this::nombreEstado);

        binder.forField(estado)
                .asRequired("Selecciona un estado")
                .bind(Empresa::getEstado, Empresa::setEstado);
    }

    private void configurarGrid() {

        grid.addColumn(Empresa::getId)
                .setHeader("ID")
                .setAutoWidth(true);

        grid.addColumn(Empresa::getNombre)
                .setHeader("Empresa")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(Empresa::getNit)
                .setHeader("NIT")
                .setAutoWidth(true);

        grid.addColumn(Empresa::getCorreo)
                .setHeader("Correo")
                .setAutoWidth(true);

        grid.addColumn(Empresa::getTelefono)
                .setHeader("Teléfono")
                .setAutoWidth(true);

        grid.addColumn(empresa -> nombreTipo(empresa.getTipo()))
                .setHeader("Tipo")
                .setAutoWidth(true);

        grid.addColumn(empresa -> nombreEstado(empresa.getEstado()))
                .setHeader("Estado")
                .setAutoWidth(true);

        grid.addComponentColumn(empresa -> {

            Button editar = new Button(
                    "Editar",
                    event -> abrirFormulario(empresa)
            );

            return editar;

        }).setHeader("Acciones").setAutoWidth(true);

        grid.setSizeFull();
    }

    private void abrirFormulario(Empresa empresa) {

        empresaActual = empresa;

        Empresa modelo = empresa == null
                ? new Empresa()
                : empresa;

        binder.readBean(modelo);

        Dialog dialogo = new Dialog();
        dialogo.setHeaderTitle(
                empresa == null ? "Registrar empresa" : "Editar empresa"
        );
        dialogo.setWidth("550px");

        VerticalLayout formulario = new VerticalLayout(
                nombre,
                nit,
                correo,
                telefono,
                direccion,
                tipo,
                estado
        );

        formulario.setPadding(true);
        formulario.setSpacing(true);

        nombre.setWidthFull();
        nit.setWidthFull();
        correo.setWidthFull();
        telefono.setWidthFull();
        direccion.setWidthFull();
        tipo.setWidthFull();
        estado.setWidthFull();

        Button guardar = new Button("Guardar", event -> {

            try {

                binder.writeBean(modelo);

                if (empresaActual == null
                        && empresaRepository.existsByNit(modelo.getNit())) {

                    Notification.show(
                            "Ya existe una empresa con ese NIT."
                    );

                    return;
                }

                if (empresaActual != null
                        && empresaRepository.existsByNitAndIdNot(
                                modelo.getNit(),
                                modelo.getId())) {

                    Notification.show(
                            "Otra empresa ya utiliza ese NIT."
                    );

                    return;
                }

                empresaRepository.save(modelo);

                Notification.show("Empresa guardada correctamente.");

                dialogo.close();
                actualizarGrid();

            } catch (Exception e) {

                Notification.show(
                        "No fue posible guardar la empresa: "
                                + e.getMessage()
                );
            }
        });

        Button cancelar = new Button(
                "Cancelar",
                event -> dialogo.close()
        );

        HorizontalLayout botones = new HorizontalLayout(
                guardar,
                cancelar
        );

        formulario.add(botones);

        dialogo.add(formulario);
        dialogo.open();
    }

    private void actualizarGrid() {

        List<Empresa> empresas = empresaRepository.findAll();

        grid.setItems(empresas);
    }

    private String nombreTipo(Empresa.TipoEmpresa tipo) {

        if (tipo == null) {
            return "";
        }

        return switch (tipo) {
            case privada -> "Privada";
            case publica -> "Pública";
            case mixta -> "Mixta";
        };
    }

    private String nombreEstado(Empresa.EstadoEmpresa estado) {

        if (estado == null) {
            return "";
        }

        return switch (estado) {
            case activo -> "Activo";
            case inactivo -> "Inactivo";
        };
    }
}
