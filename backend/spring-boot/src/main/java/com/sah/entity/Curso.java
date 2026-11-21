
package com.sah.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cursos")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_dificultad", nullable = false)
    private NivelDificultad nivelDificultad;

    @Column(nullable = false)
    private Integer duracion;

    @Column(name = "numero_modulos", nullable = false)
    private Integer numeroModulos;

    @Column(name = "porcentaje_aprobacion", nullable = false)
    private Integer porcentajeAprobacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCurso estado;

    @Column(name = "fecha_creacion",
            insertable = false,
            updatable = false)
    private LocalDateTime fechaCreacion;

    public Curso() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public NivelDificultad getNivelDificultad() {
        return nivelDificultad;
    }

    public void setNivelDificultad(NivelDificultad nivelDificultad) {
        this.nivelDificultad = nivelDificultad;
    }

    public Integer getDuracion() {
        return duracion;
    }

    public void setDuracion(Integer duracion) {
        this.duracion = duracion;
    }

    public Integer getNumeroModulos() {
        return numeroModulos;
    }

    public void setNumeroModulos(Integer numeroModulos) {
        this.numeroModulos = numeroModulos;
    }

    public Integer getPorcentajeAprobacion() {
        return porcentajeAprobacion;
    }

    public void setPorcentajeAprobacion(Integer porcentajeAprobacion) {
        this.porcentajeAprobacion = porcentajeAprobacion;
    }

    public EstadoCurso getEstado() {
        return estado;
    }

    public void setEstado(EstadoCurso estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public enum NivelDificultad {
        basico, intermedio, avanzado
    }

    public enum EstadoCurso {
        activo, inactivo
    }
}
