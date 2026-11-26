
package com.sah.service;

import com.sah.exception.DatoDuplicadoException;
import com.sah.exception.RecursoNoEncontradoException;
import com.sah.model.Empresa;
import com.sah.model.Usuario;
import com.sah.repository.EmpresaRepository;
import com.sah.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager) {

        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "No existe un usuario con id " + id + "."
                        )
                );
    }

    /**
     * Activa o desactiva un usuario. Un usuario inactivo no puede
     * iniciar sesion (lo verifica UsuarioDetailsService).
     *
     * @param correoSolicitante correo del usuario autenticado que hace
     *                          la operacion; no puede desactivarse a si mismo
     */
    public Usuario establecerEstado(
            Integer id,
            String estado,
            String correoSolicitante) {

        if (estado == null || !List.of("activo", "inactivo").contains(estado)) {
            throw new IllegalArgumentException(
                    "El estado debe ser activo o inactivo."
            );
        }

        Usuario usuario = buscarPorId(id);

        if ("inactivo".equals(estado)
                && usuario.getCorreo().equalsIgnoreCase(correoSolicitante)) {
            throw new IllegalArgumentException(
                    "No puedes desactivar tu propia cuenta."
            );
        }

        usuario.setEstado(estado);

        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public List<Empresa> listarEmpresasActivas() {
        return empresaRepository.findAll().stream()
                .filter(empresa ->
                        empresa.getEstado() == Empresa.EstadoEmpresa.activo
                )
                .toList();
    }

    public Usuario registrarUsuario(
            Usuario usuario,
            String password,
            Integer empresaId) {

        validarDatos(usuario, password, null);

        // Se guardan los mismos valores que se validaron (sin espacios)
        usuario.setNombres(usuario.getNombres().trim());
        usuario.setApellidos(usuario.getApellidos().trim());
        usuario.setCorreo(usuario.getCorreo().trim());

        usuario.setPassword(passwordEncoder.encode(password));

        asignarEmpresa(usuario, empresaId);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);

        // fecha_registro la asigna MariaDB; se recarga para devolverla
        entityManager.refresh(guardado);

        return guardado;
    }

    public Usuario actualizarUsuario(
            Integer id,
            Usuario datos,
            String nuevaPassword,
            Integer empresaId) {

        Usuario usuario = buscarPorId(id);

        validarDatos(datos, nuevaPassword, id);

        usuario.setNombres(datos.getNombres().trim());
        usuario.setApellidos(datos.getApellidos().trim());
        usuario.setCorreo(datos.getCorreo().trim());
        usuario.setRol(datos.getRol());
        usuario.setEstado(datos.getEstado());

        if (nuevaPassword != null && !nuevaPassword.isBlank()) {
            usuario.setPassword(
                    passwordEncoder.encode(nuevaPassword)
            );
        }

        asignarEmpresa(usuario, empresaId);

        return usuarioRepository.save(usuario);
    }

    private void validarDatos(
            Usuario usuario,
            String password,
            Integer idActual) {

        if (usuario.getNombres() == null
                || usuario.getNombres().isBlank()) {
            throw new IllegalArgumentException(
                    "Los nombres son obligatorios."
            );
        }

        if (usuario.getApellidos() == null
                || usuario.getApellidos().isBlank()) {
            throw new IllegalArgumentException(
                    "Los apellidos son obligatorios."
            );
        }

        if (usuario.getCorreo() == null
                || usuario.getCorreo().isBlank()) {
            throw new IllegalArgumentException(
                    "El correo electrónico es obligatorio."
            );
        }

        String correo = usuario.getCorreo().trim();

        if (!correo.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException(
                    "El correo electrónico no tiene un formato válido."
            );
        }

        if (idActual == null) {
            if (usuarioRepository.existsByCorreo(correo)) {
                throw new DatoDuplicadoException(
                        "Ya existe un usuario con ese correo."
                );
            }
        } else if (usuarioRepository.existsByCorreoAndIdNot(
                correo, idActual)) {
            throw new DatoDuplicadoException(
                    "Ese correo ya pertenece a otro usuario."
            );
        }

        if (usuario.getRol() == null
                || !List.of(
                        "admin",
                        "admin_empresa",
                        "empleado"
                ).contains(usuario.getRol())) {
            throw new IllegalArgumentException(
                    "El rol seleccionado no es válido."
            );
        }

        if (usuario.getEstado() == null
                || !List.of("activo", "inactivo")
                .contains(usuario.getEstado())) {
            throw new IllegalArgumentException(
                    "El estado seleccionado no es válido."
            );
        }

        if (idActual == null
                && (password == null || password.isBlank())) {
            throw new IllegalArgumentException(
                    "La contraseña es obligatoria."
            );
        }

        if (password != null && !password.isBlank()
                && password.length() < 8) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres."
            );
        }
    }

    private void asignarEmpresa(
            Usuario usuario,
            Integer empresaId) {

        boolean requiereEmpresa =
                !"admin".equals(usuario.getRol());

        if (empresaId == null) {
            if (requiereEmpresa) {
                throw new IllegalArgumentException(
                        "Debe seleccionar una empresa para este rol."
                );
            }

            usuario.setEmpresa(null);
            return;
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La empresa seleccionada no existe."
                        )
                );

        if (empresa.getEstado() != Empresa.EstadoEmpresa.activo) {
            throw new IllegalArgumentException(
                    "No se pueden asignar usuarios a empresas inactivas."
            );
        }

        usuario.setEmpresa(empresa);
    }
}
