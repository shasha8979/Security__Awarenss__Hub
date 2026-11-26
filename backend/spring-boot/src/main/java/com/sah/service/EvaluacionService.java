package com.sah.service;

import com.sah.exception.DatoDuplicadoException;
import com.sah.exception.RecursoNoEncontradoException;
import com.sah.model.Curso;
import com.sah.model.Evaluacion;
import com.sah.repository.CursoRepository;
import com.sah.repository.EvaluacionRepository;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio de las evaluaciones.
 *
 * Cada evaluacion pertenece a un curso (evaluaciones.curso_id).
 * No se eliminan: tienen preguntas y asignaciones; se desactivan.
 */
@Service
@Transactional
public class EvaluacionService {

    public static final int TITULO_MAXIMO = 150;

    private final EvaluacionRepository evaluacionRepository;
    private final CursoRepository cursoRepository;
    private final EntityManager entityManager;

    public EvaluacionService(
            EvaluacionRepository evaluacionRepository,
            CursoRepository cursoRepository,
            EntityManager entityManager) {
        this.evaluacionRepository = evaluacionRepository;
        this.cursoRepository = cursoRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<Evaluacion> listarEvaluaciones(Integer cursoId) {
        if (cursoId != null) {
            return evaluacionRepository.findByCursoIdOrderByIdAsc(cursoId);
        }
        return evaluacionRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public Evaluacion buscarPorId(Integer id) {
        return evaluacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una evaluación con id " + id + "."
                ));
    }

    /** Titulo del curso de una evaluacion (para mostrarlo en la respuesta). */
    @Transactional(readOnly = true)
    public String tituloCurso(Integer cursoId) {
        return cursoId == null
                ? null
                : cursoRepository.findById(cursoId).map(Curso::getTitulo).orElse(null);
    }

    public Evaluacion registrarEvaluacion(Evaluacion datos) {

        validar(datos, null);

        Curso curso = cursoRepository.findById(datos.getCursoId()).orElseThrow();

        if (curso.getEstado() != Curso.EstadoCurso.activo) {
            throw new IllegalArgumentException(
                    "No se pueden crear evaluaciones para un curso inactivo."
            );
        }

        Evaluacion evaluacion = new Evaluacion();
        copiarDatos(datos, evaluacion);
        evaluacion.setEstado(Evaluacion.EstadoEvaluacion.activo);

        Evaluacion guardada = evaluacionRepository.saveAndFlush(evaluacion);

        // fecha_creacion la asigna MariaDB; se recarga para devolverla
        entityManager.refresh(guardada);

        return guardada;
    }

    public Evaluacion actualizarEvaluacion(Integer id, Evaluacion datos) {

        Evaluacion evaluacion = buscarPorId(id);

        validar(datos, id);
        copiarDatos(datos, evaluacion);

        return evaluacionRepository.save(evaluacion);
    }

    public Evaluacion establecerEstado(
            Integer id,
            Evaluacion.EstadoEvaluacion estado) {

        if (estado == null) {
            throw new IllegalArgumentException(
                    "El estado es obligatorio (activo o inactivo)."
            );
        }

        Evaluacion evaluacion = buscarPorId(id);
        evaluacion.setEstado(estado);

        return evaluacionRepository.save(evaluacion);
    }

    private void copiarDatos(Evaluacion origen, Evaluacion destino) {
        destino.setCursoId(origen.getCursoId());
        destino.setTitulo(origen.getTitulo().trim());
        destino.setDescripcion(origen.getDescripcion().trim());
        destino.setPorcentajeAprobacion(origen.getPorcentajeAprobacion());
    }

    private void validar(Evaluacion evaluacion, Integer idActual) {

        if (evaluacion == null) {
            throw new IllegalArgumentException(
                    "No se recibieron los datos de la evaluación."
            );
        }

        if (evaluacion.getCursoId() == null) {
            throw new IllegalArgumentException(
                    "Debes indicar el curso de la evaluación."
            );
        }

        // Curso referenciado en el cuerpo: si no existe es un dato
        // invalido de la peticion (400), no un recurso de la URL (404)
        if (!cursoRepository.existsById(evaluacion.getCursoId())) {
            throw new IllegalArgumentException(
                    "El curso con id " + evaluacion.getCursoId() + " no existe."
            );
        }

        String titulo = evaluacion.getTitulo() == null
                ? ""
                : evaluacion.getTitulo().trim();

        if (titulo.isEmpty()) {
            throw new IllegalArgumentException(
                    "El título de la evaluación es obligatorio."
            );
        }

        if (titulo.length() > TITULO_MAXIMO) {
            throw new IllegalArgumentException(
                    "El título no puede superar " + TITULO_MAXIMO + " caracteres."
            );
        }

        boolean repetido = idActual == null
                ? evaluacionRepository.existsByCursoIdAndTituloIgnoreCase(
                        evaluacion.getCursoId(), titulo)
                : evaluacionRepository.existsByCursoIdAndTituloIgnoreCaseAndIdNot(
                        evaluacion.getCursoId(), titulo, idActual);

        if (repetido) {
            throw new DatoDuplicadoException(
                    "El curso ya tiene una evaluación con ese título."
            );
        }

        if (evaluacion.getDescripcion() == null
                || evaluacion.getDescripcion().isBlank()) {
            throw new IllegalArgumentException(
                    "La descripción de la evaluación es obligatoria."
            );
        }

        Integer porcentaje = evaluacion.getPorcentajeAprobacion();

        if (porcentaje == null || porcentaje < 1 || porcentaje > 100) {
            throw new IllegalArgumentException(
                    "El porcentaje de aprobación debe estar entre 1 y 100."
            );
        }
    }
}
