package com.sah.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla "resultados" (database/migracion_002_resultados.sql).
 * Un registro por cada intento de un usuario en una evaluación.
 *
 * Se guardan usuario_id y evaluacion_id como Integer, igual que
 * Evaluacion.cursoId, para mantener el estilo del proyecto.
 */
@Entity
@Table(name = "resultados")
public class Resultado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "evaluacion_id", nullable = false)
    private Integer evaluacionId;

    @Column(nullable = false)
    private Integer intento;

    @Column(name = "respuestas_correctas", nullable = false)
    private Integer respuestasCorrectas;

    @Column(name = "total_preguntas", nullable = false)
    private Integer totalPreguntas;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    /** Copia de evaluaciones.porcentaje_aprobacion en el momento del intento. */
    @Column(name = "porcentaje_requerido", nullable = false)
    private Integer porcentajeRequerido;

    @Column(nullable = false)
    private boolean aprobado;

    @Column(name = "fecha_presentacion", insertable = false, updatable = false)
    private LocalDateTime fechaPresentacion;

    @OneToMany(mappedBy = "resultado",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ResultadoRespuesta> respuestas = new ArrayList<>();

    public Resultado() {
    }

    public void agregarRespuesta(ResultadoRespuesta respuesta) {
        respuesta.setResultado(this);
        respuestas.add(respuesta);
    }

    public Integer getId() {
        return id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Integer getEvaluacionId() {
        return evaluacionId;
    }

    public void setEvaluacionId(Integer evaluacionId) {
        this.evaluacionId = evaluacionId;
    }

    public Integer getIntento() {
        return intento;
    }

    public void setIntento(Integer intento) {
        this.intento = intento;
    }

    public Integer getRespuestasCorrectas() {
        return respuestasCorrectas;
    }

    public void setRespuestasCorrectas(Integer respuestasCorrectas) {
        this.respuestasCorrectas = respuestasCorrectas;
    }

    public Integer getTotalPreguntas() {
        return totalPreguntas;
    }

    public void setTotalPreguntas(Integer totalPreguntas) {
        this.totalPreguntas = totalPreguntas;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }

    public Integer getPorcentajeRequerido() {
        return porcentajeRequerido;
    }

    public void setPorcentajeRequerido(Integer porcentajeRequerido) {
        this.porcentajeRequerido = porcentajeRequerido;
    }

    public boolean isAprobado() {
        return aprobado;
    }

    public void setAprobado(boolean aprobado) {
        this.aprobado = aprobado;
    }

    public LocalDateTime getFechaPresentacion() {
        return fechaPresentacion;
    }

    public List<ResultadoRespuesta> getRespuestas() {
        return respuestas;
    }
}
