package com.sah.dto;

import com.sah.model.Curso;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos que el cliente envia para crear o actualizar un curso.
 *
 * No incluye "id", "estado" ni "fechaCreacion": el id lo genera la base
 * de datos, la fecha la pone el servidor y el estado se cambia con su
 * propio endpoint. Si el cliente los envia, se ignoran.
 */
public record CursoRequestDTO(

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 5000, message = "La descripción no puede superar 5000 caracteres")
        String descripcion,

        @NotNull(message = "El nivel de dificultad es obligatorio "
                + "(basico, intermedio o avanzado)")
        Curso.NivelDificultad nivelDificultad,

        @NotNull(message = "La duración es obligatoria")
        @Min(value = 1, message = "La duración mínima es 1 hora")
        @Max(value = 500, message = "La duración máxima es 500 horas")
        Integer duracion,

        @NotNull(message = "El número de módulos es obligatorio")
        @Min(value = 1, message = "Debe tener al menos 1 módulo")
        @Max(value = 50, message = "No puede superar 50 módulos")
        Integer numeroModulos,

        @NotNull(message = "El porcentaje de aprobación es obligatorio")
        @Min(value = 1, message = "El porcentaje mínimo es 1")
        @Max(value = 100, message = "El porcentaje máximo es 100")
        Integer porcentajeAprobacion
) {

    /** Convierte el DTO en entidad (sin id: lo asigna la base de datos). */
    /** Formulario vacío para registrar (página web), con los valores por defecto del SQL. */
    public static CursoRequestDTO vacio() {
        return new CursoRequestDTO(null, null, Curso.NivelDificultad.basico, null, 1, 70);
    }

    /** Datos actuales para el formulario de edición. */
    public static CursoRequestDTO desde(Curso curso) {
        return new CursoRequestDTO(curso.getTitulo(), curso.getDescripcion(),
                curso.getNivelDificultad(), curso.getDuracion(),
                curso.getNumeroModulos(), curso.getPorcentajeAprobacion());
    }

    public Curso aEntidad() {
        Curso curso = new Curso();
        curso.setTitulo(titulo);
        curso.setDescripcion(descripcion);
        curso.setNivelDificultad(nivelDificultad);
        curso.setDuracion(duracion);
        curso.setNumeroModulos(numeroModulos);
        curso.setPorcentajeAprobacion(porcentajeAprobacion);
        return curso;
    }
}
