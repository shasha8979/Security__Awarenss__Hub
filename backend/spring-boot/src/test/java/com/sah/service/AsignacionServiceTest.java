package com.sah.service;

import com.sah.exception.AccesoDenegadoException;
import com.sah.exception.RecursoNoEncontradoException;
import com.sah.model.Asignacion;
import com.sah.model.Curso;
import com.sah.model.Usuario;
import com.sah.notificacion.AsignacionCreadaEvento;
import com.sah.repository.AsignacionRepository;
import com.sah.repository.CursoRepository;
import com.sah.repository.EvaluacionRepository;
import com.sah.repository.UsuarioRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de AsignacionService: avance del estudiante y
 * aviso por correo al crear una asignación.
 */
class AsignacionServiceTest {

    private AsignacionRepository asignacionRepository;
    private UsuarioRepository usuarioRepository;
    private CursoRepository cursoRepository;
    private ApplicationEventPublisher eventos;
    private AsignacionService servicio;

    private Usuario estudiante;
    private Usuario otroEstudiante;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        asignacionRepository = mock(AsignacionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        cursoRepository = mock(CursoRepository.class);
        eventos = mock(ApplicationEventPublisher.class);

        servicio = new AsignacionService(
                asignacionRepository, usuarioRepository, cursoRepository,
                mock(EvaluacionRepository.class), eventos);

        estudiante = usuario(10, "ana@empresa.com", "empleado");
        otroEstudiante = usuario(11, "luis@empresa.com", "empleado");
        admin = usuario(1, "admin@sah.com", "admin");

        for (Usuario u : List.of(estudiante, otroEstudiante, admin)) {
            when(usuarioRepository.findByCorreo(u.getCorreo())).thenReturn(Optional.of(u));
            when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }

        when(asignacionRepository.save(any(Asignacion.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private static Usuario usuario(int id, String correo, String rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombres("Nombre" + id);
        u.setApellidos("Apellido");
        u.setCorreo(correo);
        u.setRol(rol);
        u.setEstado("activo");
        return u;
    }

    private static void iniciarSesion(Usuario u) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u.getCorreo(), null, List.of()));
    }

    private Asignacion asignacionDe(Usuario dueno, Asignacion.Estado estado) {
        Asignacion a = new Asignacion();
        a.setId(100);
        a.setUsuario(dueno);
        a.setTipo(Asignacion.Tipo.curso);
        a.setEstado(estado);
        when(asignacionRepository.findById(100)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    @DisplayName("El estudiante avanza en orden: pendiente -> progreso -> completado")
    void avanzaEnOrden() {
        iniciarSesion(estudiante);
        Asignacion a = asignacionDe(estudiante, Asignacion.Estado.pendiente);

        servicio.registrarAvance(100, Asignacion.Estado.progreso);
        assertEquals(Asignacion.Estado.progreso, a.getEstado());

        servicio.registrarAvance(100, Asignacion.Estado.completado);
        assertEquals(Asignacion.Estado.completado, a.getEstado());
    }

    @Test
    @DisplayName("No se salta estados, no retrocede y no modifica una completada")
    void reglasDeTransicion() {
        iniciarSesion(estudiante);

        asignacionDe(estudiante, Asignacion.Estado.pendiente);
        IllegalArgumentException salto = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarAvance(100, Asignacion.Estado.completado));
        assertEquals("Primero debes iniciar la capacitación.", salto.getMessage());

        asignacionDe(estudiante, Asignacion.Estado.progreso);
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarAvance(100, Asignacion.Estado.pendiente));

        asignacionDe(estudiante, Asignacion.Estado.completado);
        IllegalArgumentException completada = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarAvance(100, Asignacion.Estado.completado));
        assertEquals("Esta capacitación ya está completada.", completada.getMessage());

        verify(asignacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Asignación de otro estudiante o inexistente -> 404; admin -> 403")
    void soloSusAsignaciones() {
        asignacionDe(otroEstudiante, Asignacion.Estado.pendiente);

        iniciarSesion(estudiante);
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.registrarAvance(100, Asignacion.Estado.progreso));
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.registrarAvance(999, Asignacion.Estado.progreso));

        iniciarSesion(admin);
        assertThrows(AccesoDenegadoException.class,
                () -> servicio.registrarAvance(100, Asignacion.Estado.progreso));

        verify(asignacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear una asignación publica el evento para enviar el correo")
    void crearAsignacionPublicaEvento() {
        iniciarSesion(admin);

        Curso curso = new Curso();
        curso.setId(5);
        curso.setTitulo("Phishing y correo seguro");
        curso.setEstado(Curso.EstadoCurso.activo);
        when(cursoRepository.findById(5)).thenReturn(Optional.of(curso));

        servicio.crearAsignacion(estudiante.getId(), 5, null, Asignacion.Tipo.curso);

        ArgumentCaptor<AsignacionCreadaEvento> evento =
                ArgumentCaptor.forClass(AsignacionCreadaEvento.class);
        verify(eventos).publishEvent(evento.capture());

        assertEquals("ana@empresa.com", evento.getValue().correo());
        assertEquals("el curso", evento.getValue().tipo());
        assertEquals("Phishing y correo seguro", evento.getValue().titulo());
    }
}
