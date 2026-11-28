package com.sah.dto;

import com.sah.model.Empresa;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar una empresa.
 * El id, el estado y la fecha de registro los controla el servidor.
 */
public record EmpresaRequestDTO(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El NIT es obligatorio")
        @Pattern(regexp = "^[0-9.\\-]{5,30}$",
                message = "El NIT debe tener entre 5 y 30 caracteres: números, puntos o guiones")
        String nit,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        @Pattern(regexp = "^[0-9+()\\s-]*$",
                message = "El teléfono solo puede contener números, espacios, +, - y paréntesis")
        String telefono,

        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccion,

        @NotNull(message = "El tipo es obligatorio (privada, publica o mixta)")
        Empresa.TipoEmpresa tipo
) {

    /** Formulario vacío para registrar (página web). */
    public static EmpresaRequestDTO vacio() {
        return new EmpresaRequestDTO(null, null, null, null, null, Empresa.TipoEmpresa.privada);
    }

    /** Datos actuales para el formulario de edición. */
    public static EmpresaRequestDTO desde(Empresa empresa) {
        return new EmpresaRequestDTO(empresa.getNombre(), empresa.getNit(), empresa.getCorreo(),
                empresa.getTelefono(), empresa.getDireccion(), empresa.getTipo());
    }

    public Empresa aEntidad() {
        Empresa empresa = new Empresa();
        empresa.setNombre(nombre);
        empresa.setNit(nit);
        empresa.setCorreo(correo);
        empresa.setTelefono(telefono);
        empresa.setDireccion(direccion);
        empresa.setTipo(tipo);
        return empresa;
    }
}
