package com.sah.dto;

import com.sah.model.Usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar un usuario.
 *
 * - password: obligatoria al crear; al actualizar es opcional
 *   (si no se envia, se conserva la actual). Nunca se devuelve.
 * - estado: opcional; al crear vale "activo" y al actualizar se
 *   conserva el actual. Tambien se cambia con PATCH /estado.
 * - empresaId: obligatoria para admin_empresa y empleado.
 */
public record UsuarioRequestDTO(

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden superar 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        @Size(min = 8, max = 72,
                message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = "admin|admin_empresa|empleado",
                message = "El rol debe ser admin, admin_empresa o empleado")
        String rol,

        @Pattern(regexp = "activo|inactivo",
                message = "El estado debe ser activo o inactivo")
        String estado,

        Integer empresaId
) {

    /** Formulario vacío para registrar (página web). */
    public static UsuarioRequestDTO vacio() {
        return new UsuarioRequestDTO(null, null, null, null, "empleado", "activo", null);
    }

    /** Datos actuales para el formulario de edición (sin contraseña). */
    public static UsuarioRequestDTO desde(Usuario usuario) {
        return new UsuarioRequestDTO(
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                null,
                usuario.getRol(),
                usuario.getEstado(),
                usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId()
        );
    }

    /** Convierte en entidad sin contraseña (el servicio la cifra). */
    public Usuario aEntidad(String estadoPorDefecto) {
        Usuario usuario = new Usuario();
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        usuario.setEstado(estado != null ? estado : estadoPorDefecto);
        return usuario;
    }
}
