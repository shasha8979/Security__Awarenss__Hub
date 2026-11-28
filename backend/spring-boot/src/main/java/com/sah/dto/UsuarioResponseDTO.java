package com.sah.dto;

import com.sah.model.Usuario;

import java.time.LocalDateTime;

/**
 * Representacion JSON de un usuario.
 * No incluye la contraseña ni su hash (proteccion de datos).
 */
public record UsuarioResponseDTO(
        Integer id,
        String nombres,
        String apellidos,
        String correo,
        String rol,
        String estado,
        Integer empresaId,
        String empresa,
        LocalDateTime fechaRegistro
) {

    public static UsuarioResponseDTO desde(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.getEstado(),
                usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId(),
                usuario.getEmpresa() == null ? null : usuario.getEmpresa().getNombre(),
                usuario.getFechaRegistro()
        );
    }
}
