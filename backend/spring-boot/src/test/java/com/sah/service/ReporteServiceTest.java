package com.sah.service;

import com.sah.dto.AvanceEstudianteDTO;
import com.sah.model.Asignacion;
import com.sah.model.Usuario;
import com.sah.repository.AsignacionRepository;
import com.sah.repository.CursoRepository;
import com.sah.repository.EmpresaRepository;
import com.sah.repository.EvaluacionRepository;
import com.sah.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del seguimiento de estudiantes por empresa.
 */
class ReporteServiceTest {

    private UsuarioRepository usuarioRepository;
    private AsignacionRepository asignacionRepository;
    private ReporteService servicio;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        asignacionRepository = mock(AsignacionRepository.class);
        servicio = new ReporteService(
                mock(EmpresaRepository.class),
                usuarioRepository,
                mock(CursoRepository.class),
                mock(EvaluacionRepository.class),
                asignacionRepository
        );
    }

    private Usuario estudiante(int id, String nombres, String apellidos) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setCorreo(nombres.toLowerCase() + "@empresa.com");
        u.setRol("empleado");
        u.setEstado("activo");
        return u;
    }

    private Asignacion asignacion(Usuario usuario, Asignacion.Estado estado) {
        Asignacion a = new Asignacion();
        a.setUsuario(usuario);
        a.setTipo(Asignacion.Tipo.curso);
        a.setEstado(estado);
        return a;
    }

    @Test
    @DisplayName("Sin empresa no se consulta ningún dato")
    void sinEmpresaDevuelveVacio() {
        assertTrue(servicio.generarAvancePorEstudiante(null).isEmpty());
        verify(usuarioRepository, never()).findByEmpresa_IdAndRol(any(), any());
    }

    @Test
    @DisplayName("Cuenta las asignaciones de cada estudiante por estado")
    void cuentaPorEstudiante() {
        Usuario ana = estudiante(1, "Ana", "Zapata");
        Usuario luis = estudiante(2, "Luis", "Arango");

        when(usuarioRepository.findByEmpresa_IdAndRol(7, "empleado"))
                .thenReturn(List.of(ana, luis));
        when(asignacionRepository.findByUsuario_Empresa_Id(7))
                .thenReturn(List.of(
                        asignacion(ana, Asignacion.Estado.completado),
                        asignacion(ana, Asignacion.Estado.pendiente),
                        asignacion(ana, Asignacion.Estado.progreso),
                        asignacion(ana, Asignacion.Estado.completado)
                ));

        List<AvanceEstudianteDTO> filas = servicio.generarAvancePorEstudiante(7);

        // Ordenado por apellido: Arango antes que Zapata
        assertEquals("Luis Arango", filas.get(0).nombre());
        assertEquals(0, filas.get(0).asignaciones());
        assertEquals(0, filas.get(0).porcentajeCompletado());

        AvanceEstudianteDTO filaAna = filas.get(1);
        assertEquals(4, filaAna.asignaciones());
        assertEquals(1, filaAna.pendientes());
        assertEquals(1, filaAna.enProgreso());
        assertEquals(2, filaAna.completadas());
        assertEquals(50, filaAna.porcentajeCompletado());
    }

    @Test
    @DisplayName("Solo consulta la empresa indicada")
    void soloConsultaSuEmpresa() {
        when(usuarioRepository.findByEmpresa_IdAndRol(3, "empleado"))
                .thenReturn(List.of());
        when(asignacionRepository.findByUsuario_Empresa_Id(3))
                .thenReturn(List.of());

        servicio.generarAvancePorEstudiante(3);

        verify(usuarioRepository).findByEmpresa_IdAndRol(3, "empleado");
        verify(asignacionRepository).findByUsuario_Empresa_Id(3);
        verify(usuarioRepository, never()).findAll();
        verify(asignacionRepository, never()).findAll();
    }
}
