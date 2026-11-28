package com.sah.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla "preguntas".
 *
 * Columnas: id, evaluacion_id, enunciado, orden, fecha_creacion.
 * La FK evaluacion_id tiene ON DELETE CASCADE en la base de datos.
 *
 * Se guarda evaluacion_id como Integer (igual que Evaluacion.cursoId)
 * para mantener el estilo existente del proyecto.
 */
@Entity
@Table(name = "preguntas")
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "evaluacion_id", nullable = false)
    private Integer evaluacionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Opciones de respuesta. Se guardan y eliminan junto con la pregunta.
     */
    @OneToMany(mappedBy = "pregunta",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    private List<Opcion> opciones = new ArrayList<>();

    public Pregunta() {
    }

    /** Agrega una opción manteniendo sincronizados ambos lados de la relación. */
    public void agregarOpcion(Opcion opcion) {
        opcion.setPregunta(this);
        opciones.add(opcion);
    }

    /** Elimina todas las opciones (orphanRemoval las borra de la base de datos). */
    public void limpiarOpciones() {
        opciones.forEach(opcion -> opcion.setPregunta(null));
        opciones.clear();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEvaluacionId() {
        return evaluacionId;
    }

    public void setEvaluacionId(Integer evaluacionId) {
        this.evaluacionId = evaluacionId;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public List<Opcion> getOpciones() {
        return opciones;
    }
}
