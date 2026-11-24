
package com.sah.service;

import com.sah.entity.Asignacion;
import com.sah.entity.Usuario;
import com.sah.repository.AsignacionRepository;
import com.sah.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AsignacionService {

    private final AsignacionRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;

    public AsignacionService(
            AsignacionRepository asignacionRepository,
            UsuarioRepository usuarioRepository) {
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<AsignacionView> listarAsignaciones() {
        Usuario actual = obtenerUsuarioActual();
        String rol = actual.getRol();

        if ("admin".equalsIgnoreCase(rol)) {
            return asignacionRepository.findAll();
        }

        if ("admin_empresa".equalsIgnoreCase(rol)) {
            if (actual.getEmpresa() == null) {
                throw new IllegalStateException(
                        "Tu cuenta no tiene una empresa asignada."
                );
            }

            return asignacionRepository.findByUsuario_Empresa_Id(
                    actual.getEmpresa().getId()
            );
        }

        if ("empleado".equalsIgnoreCase(rol)) {
            return asignacionRepository.findByUsuario_Id(actual.getId());
        }

        throw new IllegalStateException("Rol de usuario no autorizado.");
    }

    private Usuario obtenerUsuarioActual() {
        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion == null || !autenticacion.isAuthenticated()
                || "anonymousUser".equals(autenticacion.getName())) {
            throw new IllegalStateException("No hay una sesión autenticada.");
        }

        return usuarioRepository.findByCorreo(autenticacion.getName())
                .orElseThrow(() ->
                        new IllegalStateException("Usuario no encontrado.")
                );
    }
}
