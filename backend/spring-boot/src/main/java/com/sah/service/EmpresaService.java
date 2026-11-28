package com.sah.service;

import com.sah.exception.DatoDuplicadoException;
import com.sah.exception.RecursoNoEncontradoException;
import com.sah.model.Empresa;
import com.sah.repository.EmpresaRepository;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio de las empresas.
 *
 * Las empresas no se eliminan: sus usuarios las referencian
 * (clave foranea ON DELETE RESTRICT). Se activan o desactivan.
 */
@Service
@Transactional
public class EmpresaService {

    private static final String PATRON_CORREO =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final EmpresaRepository empresaRepository;
    private final EntityManager entityManager;

    public EmpresaService(
            EmpresaRepository empresaRepository,
            EntityManager entityManager) {
        this.empresaRepository = empresaRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<Empresa> listarEmpresas() {
        return empresaRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public Empresa buscarPorId(Integer id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una empresa con id " + id + "."
                ));
    }

    public Empresa registrarEmpresa(Empresa datos) {

        validar(datos, null);

        Empresa empresa = new Empresa();
        copiarDatos(datos, empresa);

        if (empresa.getEstado() == null) {
            empresa.setEstado(Empresa.EstadoEmpresa.activo);
        }

        Empresa guardada = empresaRepository.saveAndFlush(empresa);

        // fecha_registro la asigna MariaDB; se recarga para devolverla
        entityManager.refresh(guardada);

        return guardada;
    }

    public Empresa actualizarEmpresa(Integer id, Empresa datos) {

        Empresa empresa = buscarPorId(id);

        validar(datos, id);
        copiarDatos(datos, empresa);

        return empresaRepository.save(empresa);
    }

    public Empresa establecerEstado(Integer id, Empresa.EstadoEmpresa estado) {

        if (estado == null) {
            throw new IllegalArgumentException(
                    "El estado es obligatorio (activo o inactivo)."
            );
        }

        Empresa empresa = buscarPorId(id);
        empresa.setEstado(estado);

        return empresaRepository.save(empresa);
    }

    private void copiarDatos(Empresa origen, Empresa destino) {
        destino.setNombre(origen.getNombre().trim());
        destino.setNit(origen.getNit().trim());
        destino.setCorreo(origen.getCorreo().trim());
        destino.setTelefono(textoOpcional(origen.getTelefono()));
        destino.setDireccion(textoOpcional(origen.getDireccion()));
        destino.setTipo(origen.getTipo());

        if (origen.getEstado() != null) {
            destino.setEstado(origen.getEstado());
        }
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void validar(Empresa empresa, Integer idActual) {

        if (empresa == null) {
            throw new IllegalArgumentException(
                    "No se recibieron los datos de la empresa."
            );
        }

        if (vacio(empresa.getNombre())) {
            throw new IllegalArgumentException(
                    "El nombre de la empresa es obligatorio."
            );
        }

        if (vacio(empresa.getNit())) {
            throw new IllegalArgumentException("El NIT es obligatorio.");
        }

        String nit = empresa.getNit().trim();

        if (!nit.matches("^[0-9.\\-]{5,30}$")) {
            throw new IllegalArgumentException(
                    "El NIT solo puede contener números, puntos y guiones."
            );
        }

        boolean nitRepetido = idActual == null
                ? empresaRepository.existsByNit(nit)
                : empresaRepository.existsByNitAndIdNot(nit, idActual);

        if (nitRepetido) {
            throw new DatoDuplicadoException(
                    "Ya existe una empresa con el NIT " + nit + "."
            );
        }

        if (vacio(empresa.getCorreo())
                || !empresa.getCorreo().trim().matches(PATRON_CORREO)) {
            throw new IllegalArgumentException(
                    "El correo de la empresa no tiene un formato válido."
            );
        }

        if (empresa.getTipo() == null) {
            throw new IllegalArgumentException(
                    "El tipo de empresa es obligatorio."
            );
        }
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
