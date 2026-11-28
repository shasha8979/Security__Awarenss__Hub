package com.sah.service;

import com.sah.exception.DatoDuplicadoException;
import com.sah.exception.RecursoNoEncontradoException;
import com.sah.model.Curso;
import com.sah.repository.CursoRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de las reglas de negocio de cursos.
 * El repositorio se simula con Mockito: no se toca la base de datos.
 */
class CursoServiceTest {

    private CursoRepository repositorio;
    private CursoService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(CursoRepository.class);
        servicio = new CursoService(repositorio, mock(EntityManager.class));
        when(repositorio.save(any(Curso.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(repositorio.saveAndFlush(any(Curso.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private Curso cursoValido() {
        Curso curso = new Curso();
        curso.setTitulo("  Phishing y correo seguro  ");
        curso.setDescripcion("Identificar correos fraudulentos.");
        curso.setNivelDificultad(Curso.NivelDificultad.basico);
        curso.setDuracion(3);
        curso.setNumeroModulos(2);
        curso.setPorcentajeAprobacion(70);
        return curso;
    }

    @Test
    @DisplayName("Registrar un curso válido lo guarda activo y con el título sin espacios")
    void registrarCursoValido() {
        Curso guardado = servicio.registrarCurso(cursoValido());

        assertEquals("Phishing y correo seguro", guardado.getTitulo());
        assertEquals(Curso.EstadoCurso.activo, guardado.getEstado());
        verify(repositorio).saveAndFlush(any(Curso.class));
    }

    @Test
    @DisplayName("No permite títulos vacíos")
    void tituloObligatorio() {
        Curso curso = cursoValido();
        curso.setTitulo("   ");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrarCurso(curso));

        assertTrue(error.getMessage().contains("título"));
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("No permite dos cursos con el mismo título")
    void tituloDuplicado() {
        when(repositorio.existsByTituloIgnoreCase("Phishing y correo seguro"))
                .thenReturn(true);

        // Excepcion especifica: la API la traduce a HTTP 409
        assertThrows(DatoDuplicadoException.class,
                () -> servicio.registrarCurso(cursoValido()));
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("El porcentaje de aprobación debe estar entre 1 y 100")
    void porcentajeFueraDeRango() {
        Curso curso = cursoValido();
        curso.setPorcentajeAprobacion(150);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarCurso(curso));
    }

    @Test
    @DisplayName("La duración debe ser al menos 1 hora")
    void duracionInvalida() {
        Curso curso = cursoValido();
        curso.setDuracion(0);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarCurso(curso));
    }

    @Test
    @DisplayName("Actualizar un curso inexistente informa el error")
    void actualizarInexistente() {
        when(repositorio.findById(99)).thenReturn(Optional.empty());

        // Excepcion especifica: la API la traduce a HTTP 404
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.actualizarCurso(99, cursoValido()));
    }

    @Test
    @DisplayName("Al editar, el mismo curso puede conservar su título")
    void actualizarConservaTitulo() {
        Curso existente = cursoValido();
        existente.setId(5);
        existente.setEstado(Curso.EstadoCurso.activo);
        when(repositorio.findById(5)).thenReturn(Optional.of(existente));
        when(repositorio.existsByTituloIgnoreCaseAndIdNot(
                "Phishing y correo seguro", 5)).thenReturn(false);

        Curso cambios = cursoValido();
        cambios.setDuracion(6);

        Curso resultado = servicio.actualizarCurso(5, cambios);

        assertEquals(6, resultado.getDuracion());
        // El estado no se pierde aunque el formulario no lo envíe
        assertEquals(Curso.EstadoCurso.activo, resultado.getEstado());
    }

    @Test
    @DisplayName("Cambiar estado alterna entre activo e inactivo")
    void cambiarEstado() {
        Curso existente = cursoValido();
        existente.setId(1);
        existente.setEstado(Curso.EstadoCurso.activo);
        when(repositorio.findById(1)).thenReturn(Optional.of(existente));

        assertEquals(Curso.EstadoCurso.inactivo,
                servicio.cambiarEstado(1).getEstado());
        assertEquals(Curso.EstadoCurso.activo,
                servicio.cambiarEstado(1).getEstado());
    }
}
