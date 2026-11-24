
package com.sah.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "asignaciones",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_asignacion",
            columnNames = {
                "usuario_id",
                "curso_id",
                "evaluacion_id"
            }
        )
    }
)
public class AsignacionView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluacion_id")
    private Evaluacion evaluacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAsignacion tipo;

    @Column(
        name = "fecha_asignacion",
        insertable = false,
        updatable = false
    )
    private LocalDateTime fechaAsignacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAsignacion estado;

    public AsignacionView() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Curso getCurso() {
        return curso;
    }

    public void setCurso(Curso curso) {
        this.curso = curso;
    }

    public Evaluacion getEvaluacion() {
        return evaluacion;
    }

    public void setEvaluacion(Evaluacion evaluacion) {
        this.evaluacion = evaluacion;
    }

    public TipoAsignacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoAsignacion tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public EstadoAsignacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoAsignacion estado) {
        this.estado = estado;
    }

    public enum TipoAsignacion {
        curso,
        evaluacion
    }

    public enum EstadoAsignacion {
        pendiente,
        progreso,
        completado
    }
}
