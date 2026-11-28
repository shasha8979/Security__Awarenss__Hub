package com.sah.dto;

import com.sah.model.Empresa;

import java.time.LocalDateTime;

public record EmpresaResponseDTO(
        Integer id,
        String nombre,
        String nit,
        String correo,
        String telefono,
        String direccion,
        Empresa.TipoEmpresa tipo,
        Empresa.EstadoEmpresa estado,
        LocalDateTime fechaRegistro
) {

    public static EmpresaResponseDTO desde(Empresa empresa) {
        return new EmpresaResponseDTO(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getNit(),
                empresa.getCorreo(),
                empresa.getTelefono(),
                empresa.getDireccion(),
                empresa.getTipo(),
                empresa.getEstado(),
                empresa.getFechaRegistro()
        );
    }
}
