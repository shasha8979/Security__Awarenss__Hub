package com.sah.dto;

import com.sah.model.Usuario;

/**
 * Datos del usuario que inició sesión, para la barra lateral y la
 * barra superior de las páginas web (sin contraseña ni datos sensibles).
 */
public record UsuarioSesionDTO(
        Integer id,
        String nombres,
        String apellidos,
        String correo,
        String rol,
        Integer empresaId,
        String empresa
) {

    public static UsuarioSesionDTO desde(Usuario usuario) {
        return new UsuarioSesionDTO(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId(),
                usuario.getEmpresa() == null ? null : usuario.getEmpresa().getNombre()
        );
    }

    public boolean esAdmin() {
        return "admin".equals(rol);
    }

    public boolean esAdminEmpresa() {
        return "admin_empresa".equals(rol);
    }

    public boolean esEstudiante() {
        return "empleado".equals(rol);
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    public String inicial() {
        return nombres == null || nombres.isBlank() ? "?" : nombres.substring(0, 1).toUpperCase();
    }

    /** El rol interno "empleado" se muestra como "Estudiante". */
    public String rolEtiqueta() {
        return switch (rol) {
            case "admin" -> "Administrador";
            case "admin_empresa" -> "Administrador de empresa";
            case "empleado" -> "Estudiante";
            default -> rol;
        };
    }

    public String areaPanel() {
        return switch (rol) {
            case "admin" -> "ADMINISTRACIÓN";
            case "admin_empresa" -> "EMPRESA";
            default -> "CAPACITACIÓN";
        };
    }

    public String descripcionPanel() {
        return switch (rol) {
            case "admin" -> "Panel administrativo";
            case "admin_empresa" -> empresa == null ? "Panel de empresa" : empresa;
            default -> "Panel del estudiante";
        };
    }
}
