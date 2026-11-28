package com.sah.service;

import com.sah.exception.DatoDuplicadoException;
import com.sah.exception.RecursoNoEncontradoException;
import com.sah.model.Curso;
import com.sah.repository.CursoRepository;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio de los cursos de capacitacion.
 *
 * Los cursos no se eliminan: tienen evaluaciones y asignaciones
 * relacionadas (claves foraneas con ON DELETE RESTRICT), por eso
 * se activan o desactivan.
 */
@Service
@Transactional
public class CursoService {

    public static final int TITULO_MAXIMO = 150;
    public static final int DURACION_MAXIMA_HORAS = 500;
    public static final int MODULOS_MAXIMOS = 50;

    private final CursoRepository cursoRepository;
    private final EntityManager entityManager;

    public CursoService(
            CursoRepository cursoRepository,
            EntityManager entityManager) {
        this.cursoRepository = cursoRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<Curso> listarCursos() {
        return cursoRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public Curso buscarPorId(Integer id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un curso con id " + id + "."
                ));
    }

    public Curso registrarCurso(Curso datos) {

        validar(datos, null);

        Curso curso = new Curso();
        copiarDatos(datos, curso);

        if (curso.getEstado() == null) {
            curso.setEstado(Curso.EstadoCurso.activo);
        }

        Curso guardado = cursoRepository.saveAndFlush(curso);

        // fecha_creacion la asigna MariaDB (DEFAULT CURRENT_TIMESTAMP);
        // se recarga la fila para devolverla al cliente.
        entityManager.refresh(guardado);

        return guardado;
    }

    /** Activa o desactiva un curso (usado por la API: PATCH /estado). */
    public Curso establecerEstado(Integer id, Curso.EstadoCurso estado) {

        if (estado == null) {
            throw new IllegalArgumentException(
                    "El estado es obligatorio (activo o inactivo)."
            );
        }

        Curso curso = buscarPorId(id);
        curso.setEstado(estado);

        return cursoRepository.save(curso);
    }

    public Curso actualizarCurso(Integer id, Curso datos) {

        Curso curso = buscarPorId(id);

        validar(datos, id);
        copiarDatos(datos, curso);

        return cursoRepository.save(curso);
    }

    public Curso cambiarEstado(Integer id) {

        Curso curso = buscarPorId(id);

        curso.setEstado(
                curso.getEstado() == Curso.EstadoCurso.activo
                        ? Curso.EstadoCurso.inactivo
                        : Curso.EstadoCurso.activo
        );

        return cursoRepository.save(curso);
    }

    private void copiarDatos(Curso origen, Curso destino) {
        destino.setTitulo(origen.getTitulo().trim());
        destino.setDescripcion(origen.getDescripcion().trim());
        destino.setNivelDificultad(origen.getNivelDificultad());
        destino.setDuracion(origen.getDuracion());
        destino.setNumeroModulos(origen.getNumeroModulos());
        destino.setPorcentajeAprobacion(origen.getPorcentajeAprobacion());

        // El estado se conserva si no se envia (se cambia con cambiarEstado)
        if (origen.getEstado() != null) {
            destino.setEstado(origen.getEstado());
        }
    }

    private void validar(Curso curso, Integer idActual) {

        if (curso == null) {
            throw new IllegalArgumentException(
                    "No se recibieron los datos del curso."
            );
        }

        String titulo = curso.getTitulo() == null
                ? ""
                : curso.getTitulo().trim();

        if (titulo.isEmpty()) {
            throw new IllegalArgumentException(
                    "El título del curso es obligatorio."
            );
        }

        if (titulo.length() > TITULO_MAXIMO) {
            throw new IllegalArgumentException(
                    "El título no puede superar " + TITULO_MAXIMO + " caracteres."
            );
        }

        boolean tituloRepetido = idActual == null
                ? cursoRepository.existsByTituloIgnoreCase(titulo)
                : cursoRepository.existsByTituloIgnoreCaseAndIdNot(titulo, idActual);

        if (tituloRepetido) {
            throw new DatoDuplicadoException(
                    "Ya existe un curso con ese título."
            );
        }

        if (curso.getDescripcion() == null
                || curso.getDescripcion().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La descripción del curso es obligatoria."
            );
        }

        if (curso.getNivelDificultad() == null) {
            throw new IllegalArgumentException(
                    "Selecciona el nivel de dificultad."
            );
        }

        validarRango(curso.getDuracion(), 1, DURACION_MAXIMA_HORAS,
                "La duración debe estar entre 1 y "
                        + DURACION_MAXIMA_HORAS + " horas.");

        validarRango(curso.getNumeroModulos(), 1, MODULOS_MAXIMOS,
                "El número de módulos debe estar entre 1 y "
                        + MODULOS_MAXIMOS + ".");

        validarRango(curso.getPorcentajeAprobacion(), 1, 100,
                "El porcentaje de aprobación debe estar entre 1 y 100.");
    }

    private void validarRango(
            Integer valor,
            int minimo,
            int maximo,
            String mensaje) {

        if (valor == null || valor < minimo || valor > maximo) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}
