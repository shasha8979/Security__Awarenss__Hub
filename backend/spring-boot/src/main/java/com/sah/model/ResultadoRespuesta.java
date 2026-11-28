package com.sah.model;

import jakarta.persistence.*;

/**
 * Tabla "resultado_respuestas" (database/migracion_002_resultados.sql).
 * Opción elegida por el usuario en cada pregunta de un intento.
 *
 * pregunta_id y opcion_id pueden quedar en NULL si el administrador
 * borra la pregunta u opción después; por eso se guarda también una
 * copia del enunciado y del texto de la opción.
 */
@Entity
@Table(name = "resultado_respuestas")
public class ResultadoRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resultado_id", nullable = false)
    private Resultado resultado;

    @Column(name = "pregunta_id")
    private Integer preguntaId;

    @Column(name = "opcion_id")
    private Integer opcionId;

    @Column(name = "enunciado_pregunta", nullable = false, columnDefinition = "TEXT")
    private String enunciadoPregunta;

    /** NULL si el usuario no respondió la pregunta. */
    @Column(name = "texto_opcion", length = 255)
    private String textoOpcion;

    @Column(name = "es_correcta", nullable = false)
    private boolean esCorrecta;

    public ResultadoRespuesta() {
    }

    public Integer getId() {
        return id;
    }

    public Resultado getResultado() {
        return resultado;
    }

    public void setResultado(Resultado resultado) {
        this.resultado = resultado;
    }

    public Integer getPreguntaId() {
        return preguntaId;
    }

    public void setPreguntaId(Integer preguntaId) {
        this.preguntaId = preguntaId;
    }

    public Integer getOpcionId() {
        return opcionId;
    }

    public void setOpcionId(Integer opcionId) {
        this.opcionId = opcionId;
    }

    public String getEnunciadoPregunta() {
        return enunciadoPregunta;
    }

    public void setEnunciadoPregunta(String enunciadoPregunta) {
        this.enunciadoPregunta = enunciadoPregunta;
    }

    public String getTextoOpcion() {
        return textoOpcion;
    }

    public void setTextoOpcion(String textoOpcion) {
        this.textoOpcion = textoOpcion;
    }

    public boolean isEsCorrecta() {
        return esCorrecta;
    }

    public void setEsCorrecta(boolean esCorrecta) {
        this.esCorrecta = esCorrecta;
    }
}
