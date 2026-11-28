package com.sah.service;

import com.sah.exception.DatoDuplicadoException;
import com.sah.exception.RecursoNoEncontradoException;
import com.sah.model.Curso;
import com.sah.model.Evaluacion;
import com.sah.model.Opcion;
import com.sah.model.Pregunta;
import com.sah.repository.CursoRepository;
import com.sah.repository.EvaluacionRepository;
import com.sah.repository.PreguntaRepository;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reglas de negocio de las evaluaciones, sus preguntas y sus opciones.
 *
 * Cada evaluacion pertenece a un curso (evaluaciones.curso_id).
 * No se eliminan: tienen preguntas, asignaciones y resultados; se desactivan.
 *
 * Reglas de evaluacion (API REST y Vaadin):
 *  - curso obligatorio y existente; no se crean evaluaciones en cursos inactivos;
 *  - titulo obligatorio (max. 150) y unico dentro del curso;
 *  - descripcion obligatoria;
 *  - porcentaje de aprobacion entre 1 y 100.
 *
 * Reglas de preguntas (mismas del backend PHP, EvaluacionController::validarPreguntas):
 *  - enunciado obligatorio, al menos 2 opciones, ninguna vacia,
 *    exactamente 1 opcion correcta.
 *
 * Desde Vaadin la evaluacion se crea antes que sus preguntas, por eso:
 *  - una evaluacion creada desde Vaadin queda INACTIVA;
 *  - desde Vaadin solo se ACTIVA si tiene al menos 1 pregunta valida;
 *  - no se puede eliminar la ultima pregunta de una evaluacion activa.
 */
@Service
@Transactional
public class EvaluacionService {

    public static final int TITULO_MAXIMO = 150;
    public static final int MAX_DESCRIPCION = 5000;
    public static final int MAX_ENUNCIADO = 5000;
    public static final int MAX_TEXTO_OPCION = 255;
    public static final int MIN_OPCIONES = 2;
    public static final int PORCENTAJE_MINIMO = 1;
    public static final int PORCENTAJE_MAXIMO = 100;

    /** Datos de una opcion tal como llegan desde el formulario. */
    public record OpcionDatos(String texto, boolean correcta) {
    }

    private final EvaluacionRepository evaluacionRepository;
    private final CursoRepository cursoRepository;
    private final PreguntaRepository preguntaRepository;
    private final EntityManager entityManager;

    public EvaluacionService(
            EvaluacionRepository evaluacionRepository,
            CursoRepository cursoRepository,
            PreguntaRepository preguntaRepository,
            EntityManager entityManager) {
        this.evaluacionRepository = evaluacionRepository;
        this.cursoRepository = cursoRepository;
        this.preguntaRepository = preguntaRepository;
        this.entityManager = entityManager;
    }

    // ============================================================
    // EVALUACIONES (usado por la API REST y por Vaadin)
    // ============================================================

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

    /** Alta desde la API REST (queda activa, comportamiento original de la API). */
    public Evaluacion registrarEvaluacion(Evaluacion datos) {
        return crear(datos, Evaluacion.EstadoEvaluacion.activo);
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

    // ============================================================
    // EVALUACIONES (Vaadin)
    // ============================================================

    /** Total de preguntas por id de evaluacion (una sola consulta). */
    @Transactional(readOnly = true)
    public Map<Integer, Long> contarPreguntasPorEvaluacion() {
        Map<Integer, Long> totales = new HashMap<>();
        for (Object[] fila : preguntaRepository.contarPorEvaluacion()) {
            totales.put((Integer) fila[0], (Long) fila[1]);
        }
        return totales;
    }

    /**
     * Crea (id == null) o actualiza una evaluacion desde Vaadin.
     * Las evaluaciones nuevas quedan inactivas hasta que tengan preguntas.
     */
    public Evaluacion guardarEvaluacion(
            Integer id,
            Integer cursoId,
            String titulo,
            String descripcion,
            Integer porcentajeAprobacion) {

        Evaluacion datos = new Evaluacion();
        datos.setCursoId(cursoId);
        datos.setTitulo(titulo);
        datos.setDescripcion(descripcion);
        datos.setPorcentajeAprobacion(porcentajeAprobacion);

        return id == null
                ? crear(datos, Evaluacion.EstadoEvaluacion.inactivo)
                : actualizarEvaluacion(id, datos);
    }

    /**
     * Activa o desactiva desde Vaadin. Para activar exige al menos
     * 1 pregunta y que todas sus preguntas sean validas.
     */
    public Evaluacion cambiarEstado(Integer id) {

        Evaluacion evaluacion = buscarPorId(id);

        if (evaluacion.getEstado() == Evaluacion.EstadoEvaluacion.activo) {
            return establecerEstado(id, Evaluacion.EstadoEvaluacion.inactivo);
        }

        List<Pregunta> preguntas = preguntaRepository.findConOpcionesByEvaluacionId(id);

        if (preguntas.isEmpty()) {
            throw new ValidacionException(
                    "No puedes activar la evaluación: debe tener al menos una pregunta.");
        }

        for (int i = 0; i < preguntas.size(); i++) {
            Pregunta pregunta = preguntas.get(i);
            try {
                validarPregunta(pregunta.getEnunciado(), pregunta.getOpciones().stream()
                        .map(o -> new OpcionDatos(o.getTexto(), o.isEsCorrecta()))
                        .toList());
            } catch (ValidacionException e) {
                throw new ValidacionException(
                        "No puedes activar la evaluación. Pregunta " + (i + 1) + ": " + e.getMessage());
            }
        }

        return establecerEstado(id, Evaluacion.EstadoEvaluacion.activo);
    }

    private Evaluacion crear(Evaluacion datos, Evaluacion.EstadoEvaluacion estadoInicial) {

        validar(datos, null);

        Curso curso = cursoRepository.findById(datos.getCursoId()).orElseThrow();

        if (curso.getEstado() != Curso.EstadoCurso.activo) {
            throw new IllegalArgumentException(
                    "No se pueden crear evaluaciones para un curso inactivo."
            );
        }

        Evaluacion evaluacion = new Evaluacion();
        copiarDatos(datos, evaluacion);
        evaluacion.setEstado(estadoInicial);

        Evaluacion guardada = evaluacionRepository.saveAndFlush(evaluacion);

        // fecha_creacion la asigna MariaDB; se recarga para devolverla
        entityManager.refresh(guardada);

        return guardada;
    }

    private void copiarDatos(Evaluacion origen, Evaluacion destino) {
        destino.setCursoId(origen.getCursoId());
        destino.setTitulo(origen.getTitulo().trim());
        destino.setDescripcion(origen.getDescripcion().trim());
        destino.setPorcentajeAprobacion(origen.getPorcentajeAprobacion());
    }

    /** Reglas que necesitan la base de datos, despues de las reglas basicas. */
    private void validar(Evaluacion evaluacion, Integer idActual) {

        if (evaluacion == null) {
            throw new IllegalArgumentException(
                    "No se recibieron los datos de la evaluación."
            );
        }

        validarDatosEvaluacion(
                evaluacion.getCursoId(),
                evaluacion.getTitulo(),
                evaluacion.getDescripcion(),
                evaluacion.getPorcentajeAprobacion());

        // Curso referenciado en el cuerpo: si no existe es un dato
        // invalido de la peticion (400), no un recurso de la URL (404)
        if (!cursoRepository.existsById(evaluacion.getCursoId())) {
            throw new IllegalArgumentException(
                    "El curso con id " + evaluacion.getCursoId() + " no existe."
            );
        }

        String titulo = evaluacion.getTitulo().trim();

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
    }

    // ============================================================
    // PREGUNTAS Y OPCIONES
    // ============================================================

    @Transactional(readOnly = true)
    public List<Pregunta> listarPreguntas(Integer evaluacionId) {
        return preguntaRepository.findConOpcionesByEvaluacionId(evaluacionId);
    }

    /**
     * Crea (preguntaId == null) o actualiza una pregunta con todas sus opciones.
     * Al actualizar, las opciones anteriores se reemplazan por las nuevas
     * (mismo comportamiento que el backend PHP).
     */
    public Pregunta guardarPregunta(
            Integer evaluacionId,
            Integer preguntaId,
            String enunciado,
            List<OpcionDatos> opciones) {

        validarPregunta(enunciado, opciones);

        if (!evaluacionRepository.existsById(evaluacionId)) {
            throw new RecursoNoEncontradoException(
                    "No existe una evaluación con id " + evaluacionId + ".");
        }

        Pregunta pregunta;

        if (preguntaId == null) {
            pregunta = new Pregunta();
            pregunta.setEvaluacionId(evaluacionId);
            pregunta.setOrden(preguntaRepository.findMaxOrdenByEvaluacionId(evaluacionId) + 1);
        } else {
            pregunta = preguntaRepository.findConOpcionesById(preguntaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("La pregunta no existe."));

            if (!pregunta.getEvaluacionId().equals(evaluacionId)) {
                throw new ValidacionException("La pregunta no pertenece a esta evaluación.");
            }

            pregunta.limpiarOpciones();
        }

        pregunta.setEnunciado(enunciado.trim());

        for (int i = 0; i < opciones.size(); i++) {
            OpcionDatos datos = opciones.get(i);
            pregunta.agregarOpcion(new Opcion(datos.texto().trim(), datos.correcta(), i + 1));
        }

        return preguntaRepository.save(pregunta);
    }

    /** Elimina una pregunta (sus opciones se borran en cascada) y renumera el orden. */
    public void eliminarPregunta(Integer preguntaId) {

        Pregunta pregunta = preguntaRepository.findById(preguntaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La pregunta no existe."));

        Integer evaluacionId = pregunta.getEvaluacionId();
        Evaluacion evaluacion = buscarPorId(evaluacionId);

        if (evaluacion.getEstado() == Evaluacion.EstadoEvaluacion.activo
                && preguntaRepository.countByEvaluacionId(evaluacionId) <= 1) {

            throw new ValidacionException(
                    "No puedes eliminar la única pregunta de una evaluación activa. "
                            + "Desactiva la evaluación primero.");
        }

        preguntaRepository.delete(pregunta);
        preguntaRepository.flush();

        renumerar(preguntaRepository.findByEvaluacionIdOrderByOrdenAscIdAsc(evaluacionId));
    }

    /** Mueve una pregunta una posicion arriba (-1) o abajo (+1). */
    public void moverPregunta(Integer preguntaId, int direccion) {

        Pregunta pregunta = preguntaRepository.findById(preguntaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La pregunta no existe."));

        List<Pregunta> preguntas =
                preguntaRepository.findByEvaluacionIdOrderByOrdenAscIdAsc(pregunta.getEvaluacionId());

        int actual = -1;
        for (int i = 0; i < preguntas.size(); i++) {
            if (preguntas.get(i).getId().equals(preguntaId)) {
                actual = i;
                break;
            }
        }

        int destino = actual + Integer.signum(direccion);

        if (actual < 0 || destino < 0 || destino >= preguntas.size()) {
            return;
        }

        preguntas.set(actual, preguntas.get(destino));
        preguntas.set(destino, pregunta);

        renumerar(preguntas);
    }

    private void renumerar(List<Pregunta> preguntas) {
        for (int i = 0; i < preguntas.size(); i++) {
            preguntas.get(i).setOrden(i + 1);
        }
        preguntaRepository.saveAll(preguntas);
    }

    // ============================================================
    // VALIDACIONES SIN BASE DE DATOS (probadas en EvaluacionServiceTest)
    // ============================================================

    public static void validarDatosEvaluacion(
            Integer cursoId,
            String titulo,
            String descripcion,
            Integer porcentajeAprobacion) {

        if (cursoId == null) {
            throw new ValidacionException("Debes indicar el curso de la evaluación.");
        }
        if (esVacio(titulo)) {
            throw new ValidacionException("El título de la evaluación es obligatorio.");
        }
        if (titulo.trim().length() > TITULO_MAXIMO) {
            throw new ValidacionException(
                    "El título no puede superar " + TITULO_MAXIMO + " caracteres.");
        }
        if (esVacio(descripcion)) {
            throw new ValidacionException("La descripción de la evaluación es obligatoria.");
        }
        if (descripcion.trim().length() > MAX_DESCRIPCION) {
            throw new ValidacionException(
                    "La descripción no puede superar " + MAX_DESCRIPCION + " caracteres.");
        }
        if (porcentajeAprobacion == null
                || porcentajeAprobacion < PORCENTAJE_MINIMO
                || porcentajeAprobacion > PORCENTAJE_MAXIMO) {
            throw new ValidacionException(
                    "El porcentaje de aprobación debe estar entre "
                            + PORCENTAJE_MINIMO + " y " + PORCENTAJE_MAXIMO + ".");
        }
    }

    public static void validarPregunta(String enunciado, List<OpcionDatos> opciones) {

        if (esVacio(enunciado)) {
            throw new ValidacionException("La pregunta no tiene enunciado.");
        }
        if (enunciado.trim().length() > MAX_ENUNCIADO) {
            throw new ValidacionException(
                    "El enunciado no puede superar " + MAX_ENUNCIADO + " caracteres.");
        }
        if (opciones == null || opciones.size() < MIN_OPCIONES) {
            throw new ValidacionException("La pregunta debe tener al menos " + MIN_OPCIONES + " opciones.");
        }

        int correctas = 0;

        for (OpcionDatos opcion : opciones) {
            if (opcion == null || esVacio(opcion.texto())) {
                throw new ValidacionException("Una opción de la pregunta está vacía.");
            }
            if (opcion.texto().trim().length() > MAX_TEXTO_OPCION) {
                throw new ValidacionException(
                        "Cada opción puede tener máximo " + MAX_TEXTO_OPCION + " caracteres.");
            }
            if (opcion.correcta()) {
                correctas++;
            }
        }

        if (correctas != 1) {
            throw new ValidacionException(
                    "La pregunta debe tener exactamente una opción correcta (tiene " + correctas + ").");
        }
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
