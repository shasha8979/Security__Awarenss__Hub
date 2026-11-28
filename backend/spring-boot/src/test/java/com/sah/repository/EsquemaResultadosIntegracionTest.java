package com.sah.repository;

import com.sah.model.Curso;
import com.sah.model.Evaluacion;
import com.sah.model.Opcion;
import com.sah.model.Pregunta;
import com.sah.model.Resultado;
import com.sah.model.ResultadoRespuesta;
import com.sah.model.Usuario;
import com.sah.service.EvaluacionService;
import com.sah.service.EvaluacionService.OpcionDatos;
import com.sah.service.ValidacionException;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración contra la MariaDB local (security_awareness_hub).
 *
 * - Hibernate valida (ddl-auto=validate) que las entidades coinciden
 *   con las tablas reales, incluidas resultados y resultado_respuestas.
 * - Cada prueba corre en una transacción que se REVIERTE al final:
 *   no deja datos en la base.
 *
 * Requisitos: MariaDB encendido, base importada y
 * database/migracion_002_resultados.sql aplicada.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EvaluacionService.class)
class EsquemaResultadosIntegracionTest {

    @Autowired private EntityManager em;
    @Autowired private CursoRepository cursoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EvaluacionRepository evaluacionRepository;
    @Autowired private PreguntaRepository preguntaRepository;
    @Autowired private ResultadoRepository resultadoRepository;
    @Autowired private EvaluacionService evaluacionService;

    private Usuario usuario;
    private Evaluacion evaluacion;
    private Pregunta pregunta;

    @BeforeEach
    void prepararDatos() {

        Curso curso = cursoRepository.findAll().stream()
                .filter(c -> c.getEstado() == Curso.EstadoCurso.activo)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La base no tiene cursos activos."));

        usuario = usuarioRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("La base no tiene usuarios."));

        // Titulo unico: la regla de main no permite titulos repetidos en un curso
        evaluacion = evaluacionService.guardarEvaluacion(
                null, curso.getId(), "Prueba integración " + System.nanoTime(),
                "Descripción de prueba", 60);

        pregunta = evaluacionService.guardarPregunta(evaluacion.getId(), null,
                "¿Qué es phishing?",
                List.of(new OpcionDatos("Un engaño", true), new OpcionDatos("Un antivirus", false)));

        em.flush();
    }

    private Resultado registrarIntento() {

        Opcion correcta = pregunta.getOpciones().stream()
                .filter(Opcion::isEsCorrecta).findFirst().orElseThrow();

        Resultado resultado = new Resultado();
        resultado.setUsuarioId(usuario.getId());
        resultado.setEvaluacionId(evaluacion.getId());
        resultado.setIntento(resultadoRepository.findUltimoIntento(usuario.getId(), evaluacion.getId()) + 1);
        resultado.setRespuestasCorrectas(1);
        resultado.setTotalPreguntas(1);
        resultado.setPorcentaje(new BigDecimal("100.00"));
        resultado.setPorcentajeRequerido(evaluacion.getPorcentajeAprobacion());
        resultado.setAprobado(true);

        ResultadoRespuesta respuesta = new ResultadoRespuesta();
        respuesta.setPreguntaId(pregunta.getId());
        respuesta.setOpcionId(correcta.getId());
        respuesta.setEnunciadoPregunta(pregunta.getEnunciado());
        respuesta.setTextoOpcion(correcta.getTexto());
        respuesta.setEsCorrecta(true);
        resultado.agregarRespuesta(respuesta);

        resultadoRepository.saveAndFlush(resultado);
        return resultado;
    }

    @Test
    void evaluacionNuevaQuedaInactivaYSusPreguntasSeGuardan() {

        em.clear();

        assertEquals(Evaluacion.EstadoEvaluacion.inactivo,
                evaluacionRepository.findById(evaluacion.getId()).orElseThrow().getEstado());

        List<Pregunta> preguntas = preguntaRepository.findConOpcionesByEvaluacionId(evaluacion.getId());
        assertEquals(1, preguntas.size());
        assertEquals(2, preguntas.get(0).getOpciones().size());
        assertEquals(1, preguntas.get(0).getOrden());
    }

    @Test
    void guardaIntentoConRespuestasYCalculaNumeroDeIntento() {

        Resultado primero = registrarIntento();
        Resultado segundo = registrarIntento();

        assertEquals(1, primero.getIntento());
        assertEquals(2, segundo.getIntento());

        em.clear();

        Resultado leido = resultadoRepository.findConRespuestasById(primero.getId()).orElseThrow();
        assertEquals(1, leido.getRespuestas().size());
        assertTrue(leido.getRespuestas().get(0).isEsCorrecta());
        assertEquals(0, new BigDecimal("100").compareTo(leido.getPorcentaje()));
        assertNotNull(leido.getFechaPresentacion());
    }

    @Test
    void editarPreguntaDespuesDeUnIntentoConservaElHistorial() {

        Resultado resultado = registrarIntento();

        // Reemplazar las opciones borra las anteriores: la FK queda en NULL (ON DELETE SET NULL).
        evaluacionService.guardarPregunta(evaluacion.getId(), pregunta.getId(),
                "¿Qué es phishing? (editada)",
                List.of(new OpcionDatos("Suplantación", true), new OpcionDatos("Firewall", false)));
        em.flush();
        em.clear();

        ResultadoRespuesta respuesta = resultadoRepository.findConRespuestasById(resultado.getId())
                .orElseThrow().getRespuestas().get(0);

        assertNull(respuesta.getOpcionId());
        assertEquals("Un engaño", respuesta.getTextoOpcion());
        assertEquals("¿Qué es phishing?", respuesta.getEnunciadoPregunta());
    }

    @Test
    void soloSeActivaConPreguntasYSeDesactivaDespues() {

        Evaluacion sinPreguntas = evaluacionService.guardarEvaluacion(
                null, evaluacion.getCursoId(), "Sin preguntas " + System.nanoTime(),
                "Descripción", 70);

        ValidacionException error = assertThrows(ValidacionException.class,
                () -> evaluacionService.cambiarEstado(sinPreguntas.getId()));
        assertTrue(error.getMessage().contains("al menos una pregunta"));

        assertEquals(Evaluacion.EstadoEvaluacion.activo,
                evaluacionService.cambiarEstado(evaluacion.getId()).getEstado());
        assertEquals(Evaluacion.EstadoEvaluacion.inactivo,
                evaluacionService.cambiarEstado(evaluacion.getId()).getEstado());
    }

    @Test
    void noSePermiteTituloRepetidoEnElMismoCurso() {

        assertThrows(IllegalArgumentException.class, () -> evaluacionService.guardarEvaluacion(
                null, evaluacion.getCursoId(), evaluacion.getTitulo(), "Otra", 70));
    }
}
