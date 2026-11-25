
package com.sah.service;

import com.sah.entity.Asignacion;
import com.sah.entity.Usuario;
import com.sah.entity.Curso;
import com.sah.entity.Evaluacion;

import com.sah.repository.AsignacionRepository;
import com.sah.repository.UsuarioRepository;
import com.sah.repository.CursoRepository;
import com.sah.repository.EvaluacionRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AsignacionService {

    private final AsignacionRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final EvaluacionRepository evaluacionRepository;

    public AsignacionService(
            AsignacionRepository asignacionRepository,
            UsuarioRepository usuarioRepository,
            CursoRepository cursoRepository,
            EvaluacionRepository evaluacionRepository) {

        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.evaluacionRepository = evaluacionRepository;
    }

    // CONSULTAR ASIGNACIONES SEGUN EL ROL

    public List<Asignacion> listarAsignaciones() {

        Usuario actual = obtenerUsuarioActual();
        String rol = actual.getRol();

        if ("admin".equalsIgnoreCase(rol)) {
            return asignacionRepository.findAll();
        }

        if ("admin_empresa".equalsIgnoreCase(rol)) {

            if (actual.getEmpresa() == null) {
                throw new IllegalStateException(
                        "Tu cuenta no tiene una empresa asignada."
                );
            }

            return asignacionRepository.findByUsuario_Empresa_Id(
                    actual.getEmpresa().getId()
            );
        }

        if ("empleado".equalsIgnoreCase(rol)) {
            return asignacionRepository.findByUsuario_Id(actual.getId());
        }

        throw new IllegalStateException(
                "Rol de usuario no autorizado."
        );
    }

    // CREAR ASIGNACION

    @Transactional
    public Asignacion crearAsignacion(
            Integer usuarioId,
            Integer cursoId,
            Integer evaluacionId,
            Asignacion.Tipo tipo) {

        Usuario actual = obtenerUsuarioActual();

        // Validar el rol del usuario que realiza la operación.

        String rol = actual.getRol();

        if (!"admin".equalsIgnoreCase(rol)
                && !"admin_empresa".equalsIgnoreCase(rol)) {

            throw new IllegalStateException(
                    "No tienes permisos para crear asignaciones."
            );
        }

        // Validar el tipo de asignación.

        if (tipo == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar un tipo de asignación."
            );
        }

        if (tipo == Asignacion.Tipo.curso
                && (cursoId == null || evaluacionId != null)) {

            throw new IllegalArgumentException(
                    "Para asignar un curso debes seleccionar únicamente un curso."
            );
        }

        if (tipo == Asignacion.Tipo.evaluacion
                && (evaluacionId == null || cursoId != null)) {

            throw new IllegalArgumentException(
                    "Para asignar una evaluación debes seleccionar únicamente una evaluación."
            );
        }

        // Buscar al empleado.

        Usuario empleado = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El empleado seleccionado no existe."
                        )
                );

        if (!"empleado".equalsIgnoreCase(empleado.getRol())) {
            throw new IllegalArgumentException(
                    "Las asignaciones solo pueden realizarse a empleados."
            );
        }

        if (!"activo".equalsIgnoreCase(empleado.getEstado())) {
            throw new IllegalArgumentException(
                    "No puedes asignar contenido a un empleado inactivo."
            );
        }

        // Validar que el administrador de empresa
        // solo asigne a empleados de su propia empresa.

        if ("admin_empresa".equalsIgnoreCase(rol)) {

            if (actual.getEmpresa() == null
                    || empleado.getEmpresa() == null
                    || !actual.getEmpresa().getId().equals(
                            empleado.getEmpresa().getId())) {

                throw new IllegalStateException(
                        "No puedes asignar contenido a empleados de otra empresa."
                );
            }
        }

        Asignacion asignacion = new Asignacion();

        asignacion.setUsuario(empleado);
        asignacion.setEstado(Asignacion.Estado.pendiente);
        asignacion.setTipo(tipo);

        // ASIGNACION DE CURSO

        if (tipo == Asignacion.Tipo.curso) {

            Curso curso = cursoRepository.findById(cursoId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "El curso seleccionado no existe."
                            )
                    );

            if (curso.getEstado() != Curso.EstadoCurso.activo) {
                throw new IllegalArgumentException(
                        "No puedes asignar un curso inactivo."
                );
            }

            if (asignacionRepository
                    .existsByUsuario_IdAndCurso_Id(usuarioId, cursoId)) {

                throw new IllegalStateException(
                        "Este empleado ya tiene asignado ese curso."
                );
            }

            asignacion.setCurso(curso);
            asignacion.setEvaluacion(null);
        }

        // ASIGNACION DE EVALUACION

        if (tipo == Asignacion.Tipo.evaluacion) {

            Evaluacion evaluacion =
                    evaluacionRepository.findById(evaluacionId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "La evaluación seleccionada no existe."
                                    )
                            );

            if (evaluacion.getEstado()
                    != Evaluacion.EstadoEvaluacion.activo) {

                throw new IllegalArgumentException(
                        "No puedes asignar una evaluación inactiva."
                );
            }

            if (asignacionRepository
                    .existsByUsuario_IdAndEvaluacion_Id(
                            usuarioId,
                            evaluacionId)) {

                throw new IllegalStateException(
                        "Este empleado ya tiene asignada esa evaluación."
                );
            }

            evaluacionRepository.findById(evaluacion.getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "La evaluación no existe."
                            )
                    );

            Curso cursoRelacionado = cursoRepository
                    .findById(evaluacion.getCursoId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "El curso asociado a la evaluación no existe."
                            )
                    );

            if (cursoRelacionado.getEstado()
                    != Curso.EstadoCurso.activo) {

                throw new IllegalArgumentException(
                        "No puedes asignar evaluaciones de cursos inactivos."
                );
            }

            asignacion.setEvaluacion(evaluacion);
            asignacion.setCurso(null);
        }

        return asignacionRepository.save(asignacion);
    }

    // OBTENER USUARIO AUTENTICADO

    private Usuario obtenerUsuarioActual() {

        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion == null
                || !autenticacion.isAuthenticated()
                || "anonymousUser".equals(autenticacion.getName())) {

            throw new IllegalStateException(
                    "No hay una sesión autenticada."
            );
        }

        return usuarioRepository.findByCorreo(
                autenticacion.getName()
        ).orElseThrow(() ->
                new IllegalStateException(
                        "Usuario no encontrado."
                )
        );
    }
}
