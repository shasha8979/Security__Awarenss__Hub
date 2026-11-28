package com.sah.model;

import jakarta.persistence.*;

/**
 * Tabla "opciones".
 *
 * Columnas: id, pregunta_id, texto VARCHAR(255), es_correcta TINYINT(1), orden.
 * La FK pregunta_id tiene ON DELETE CASCADE en la base de datos.
 */
@Entity
@Table(name = "opciones")
public class Opcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @Column(nullable = false, length = 255)
    private String texto;

    @Column(name = "es_correcta", nullable = false)
    private boolean esCorrecta;

    @Column(nullable = false)
    private Integer orden;

    public Opcion() {
    }

    public Opcion(String texto, boolean esCorrecta, Integer orden) {
        this.texto = texto;
        this.esCorrecta = esCorrecta;
        this.orden = orden;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Pregunta getPregunta() {
        return pregunta;
    }

    public void setPregunta(Pregunta pregunta) {
        this.pregunta = pregunta;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isEsCorrecta() {
        return esCorrecta;
    }

    public void setEsCorrecta(boolean esCorrecta) {
        this.esCorrecta = esCorrecta;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
}
