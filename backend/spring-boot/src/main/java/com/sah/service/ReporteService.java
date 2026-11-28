package com.sah.service;

import com.sah.dto.AvanceCursoDTO;
import com.sah.dto.AvanceEmpresaDTO;
import com.sah.dto.AvanceEstudianteDTO;
import com.sah.dto.ResumenReporteDTO;
import com.sah.model.Asignacion;
import com.sah.model.Curso;
import com.sah.model.Empresa;
import com.sah.model.Evaluacion;
import com.sah.model.Usuario;
import com.sah.repository.AsignacionRepository;
import com.sah.repository.CursoRepository;
import com.sah.repository.EmpresaRepository;
import com.sah.repository.EvaluacionRepository;
import com.sah.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calcula los reportes de la plataforma a partir de los datos
 * persistidos. No inventa cifras: si no hay registros, los
 * indicadores quedan en cero.
 *
 * Los resultados de evaluaciones (notas) no se reportan porque
 * todavia no existe una tabla de resultados en la base de datos.
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final String ROL_ADMIN = "admin";
    private static final String ROL_ADMIN_EMPRESA = "admin_empresa";
    private static final String ROL_ESTUDIANTE = "empleado";
    private static final String ESTADO_ACTIVO = "activo";

    // Clave para agrupar estudiantes sin empresa (los id reales son > 0)
    private static final Integer SIN_EMPRESA = -1;

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final AsignacionRepository asignacionRepository;

    public ReporteService(
            EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository,
            CursoRepository cursoRepository,
            EvaluacionRepository evaluacionRepository,
            AsignacionRepository asignacionRepository) {

        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.asignacionRepository = asignacionRepository;
    }

    public ResumenReporteDTO generarResumen() {

        List<Empresa> empresas = empresaRepository.findAll();
        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Curso> cursos = cursoRepository.findAll();
        List<Evaluacion> evaluaciones = evaluacionRepository.findAll();
        List<Asignacion> asignaciones = asignacionRepository.findAll();

        return new ResumenReporteDTO(
                empresas.size(),
                empresas.stream()
                        .filter(e -> e.getEstado() == Empresa.EstadoEmpresa.activo)
                        .count(),
                usuarios.size(),
                contarRol(usuarios, ROL_ADMIN),
                contarRol(usuarios, ROL_ADMIN_EMPRESA),
                contarRol(usuarios, ROL_ESTUDIANTE),
                usuarios.stream()
                        .filter(u -> ROL_ESTUDIANTE.equalsIgnoreCase(u.getRol()))
                        .filter(u -> ESTADO_ACTIVO.equalsIgnoreCase(u.getEstado()))
                        .count(),
                cursos.size(),
                cursos.stream()
                        .filter(c -> c.getEstado() == Curso.EstadoCurso.activo)
                        .count(),
                evaluaciones.size(),
                evaluaciones.stream()
                        .filter(e -> e.getEstado()
                                == Evaluacion.EstadoEvaluacion.activo)
                        .count(),
                asignaciones.size(),
                contarEstado(asignaciones, Asignacion.Estado.pendiente),
                contarEstado(asignaciones, Asignacion.Estado.progreso),
                contarEstado(asignaciones, Asignacion.Estado.completado)
        );
    }

    public List<AvanceCursoDTO> generarAvancePorCurso() {

        Map<Integer, List<Asignacion>> porCurso =
                asignacionRepository.findAll().stream()
                        .filter(a -> a.getTipo() == Asignacion.Tipo.curso)
                        .filter(a -> a.getCurso() != null)
                        .collect(Collectors.groupingBy(a -> a.getCurso().getId()));

        return cursoRepository.findAll().stream()
                .sorted(Comparator.comparing(Curso::getId))
                .map(curso -> {
                    List<Asignacion> lista =
                            porCurso.getOrDefault(curso.getId(), List.of());

                    return new AvanceCursoDTO(
                            curso.getId(),
                            curso.getTitulo(),
                            curso.getEstado() == Curso.EstadoCurso.activo
                                    ? "Activo"
                                    : "Inactivo",
                            lista.size(),
                            contarEstado(lista, Asignacion.Estado.pendiente),
                            contarEstado(lista, Asignacion.Estado.progreso),
                            contarEstado(lista, Asignacion.Estado.completado)
                    );
                })
                .toList();
    }

    public List<AvanceEmpresaDTO> generarAvancePorEmpresa() {

        // Usuarios con su empresa cargada (EntityGraph en el repositorio)
        Map<Integer, Usuario> usuariosPorId = usuarioRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));

        Map<Integer, List<Usuario>> estudiantesPorEmpresa =
                usuariosPorId.values().stream()
                        .filter(u -> ROL_ESTUDIANTE.equalsIgnoreCase(u.getRol()))
                        .collect(Collectors.groupingBy(this::idEmpresa));

        Map<Integer, List<Asignacion>> asignacionesPorEmpresa =
                asignacionRepository.findAll().stream()
                        .collect(Collectors.groupingBy(a ->
                                idEmpresa(usuariosPorId.get(a.getUsuario().getId()))
                        ));

        List<AvanceEmpresaDTO> filas = new ArrayList<>();

        empresaRepository.findAll().stream()
                .sorted(Comparator.comparing(Empresa::getId))
                .forEach(empresa -> filas.add(crearFilaEmpresa(
                        empresa.getId(),
                        empresa.getNombre(),
                        estudiantesPorEmpresa,
                        asignacionesPorEmpresa
                )));

        // Estudiantes que no pertenecen a ninguna empresa
        if (estudiantesPorEmpresa.containsKey(SIN_EMPRESA)
                || asignacionesPorEmpresa.containsKey(SIN_EMPRESA)) {

            filas.add(crearFilaEmpresa(
                    null,
                    "Sin empresa asignada",
                    estudiantesPorEmpresa,
                    asignacionesPorEmpresa
            ));
        }

        return filas;
    }

    /**
     * Avance de cada estudiante de una empresa. El id de la empresa debe
     * venir del usuario autenticado, nunca de un dato enviado por el
     * cliente, para que un administrador de empresa no vea otras empresas.
     */
    /**
     * Exportación de reportes a CSV (equivale a export_reporte.php).
     * Separador ";" y BOM UTF-8 para que Excel abra bien tildes y ñ.
     *
     * @param tipo "cursos" o "empresas"
     */
    public String exportarCsv(String tipo) {

        StringBuilder csv = new StringBuilder("﻿");

        if ("cursos".equals(tipo)) {
            csv.append("Curso;Estado;Asignados;Pendientes;En progreso;Completados;% completado\n");
            for (AvanceCursoDTO c : generarAvancePorCurso()) {
                csv.append(celda(c.curso())).append(';').append(c.estadoCurso()).append(';')
                        .append(c.asignados()).append(';').append(c.pendientes()).append(';')
                        .append(c.enProgreso()).append(';').append(c.completados()).append(';')
                        .append(porcentaje(c.completados(), c.asignados())).append('\n');
            }
            return csv.toString();
        }

        if ("empresas".equals(tipo)) {
            csv.append("Empresa;Estudiantes;Asignaciones;Pendientes;En progreso;Completadas;% completado\n");
            for (AvanceEmpresaDTO e : generarAvancePorEmpresa()) {
                csv.append(celda(e.empresa())).append(';').append(e.estudiantes()).append(';')
                        .append(e.asignaciones()).append(';').append(e.pendientes()).append(';')
                        .append(e.enProgreso()).append(';').append(e.completadas()).append(';')
                        .append(porcentaje(e.completadas(), e.asignaciones())).append('\n');
            }
            return csv.toString();
        }

        throw new IllegalArgumentException("El tipo de reporte debe ser cursos o empresas.");
    }

    private static long porcentaje(long parte, long total) {
        return total == 0 ? 0 : parte * 100 / total;
    }

    /** Texto entre comillas si contiene ; o comillas (formato CSV). */
    private static String celda(String texto) {
        if (texto == null) {
            return "";
        }
        if (texto.contains(";") || texto.contains("\"") || texto.contains("\n")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    public List<AvanceEstudianteDTO> generarAvancePorEstudiante(Integer empresaId) {

        if (empresaId == null) {
            return List.of();
        }

        Map<Integer, List<Asignacion>> porUsuario =
                asignacionRepository.findByUsuario_Empresa_Id(empresaId).stream()
                        .collect(Collectors.groupingBy(a -> a.getUsuario().getId()));

        return usuarioRepository.findByEmpresa_IdAndRol(empresaId, ROL_ESTUDIANTE)
                .stream()
                .sorted(Comparator.comparing(Usuario::getApellidos,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Usuario::getNombres,
                                String.CASE_INSENSITIVE_ORDER))
                .map(estudiante -> {
                    List<Asignacion> lista =
                            porUsuario.getOrDefault(estudiante.getId(), List.of());

                    return new AvanceEstudianteDTO(
                            estudiante.getId(),
                            estudiante.getNombres() + " " + estudiante.getApellidos(),
                            estudiante.getCorreo(),
                            ESTADO_ACTIVO.equalsIgnoreCase(estudiante.getEstado())
                                    ? "Activo"
                                    : "Inactivo",
                            lista.size(),
                            contarEstado(lista, Asignacion.Estado.pendiente),
                            contarEstado(lista, Asignacion.Estado.progreso),
                            contarEstado(lista, Asignacion.Estado.completado)
                    );
                })
                .toList();
    }

    private AvanceEmpresaDTO crearFilaEmpresa(
            Integer empresaId,
            String nombre,
            Map<Integer, List<Usuario>> estudiantes,
            Map<Integer, List<Asignacion>> asignaciones) {

        Integer clave = empresaId == null ? SIN_EMPRESA : empresaId;
        List<Asignacion> lista = asignaciones.getOrDefault(clave, List.of());

        return new AvanceEmpresaDTO(
                empresaId,
                nombre,
                estudiantes.getOrDefault(clave, List.of()).size(),
                lista.size(),
                contarEstado(lista, Asignacion.Estado.pendiente),
                contarEstado(lista, Asignacion.Estado.progreso),
                contarEstado(lista, Asignacion.Estado.completado)
        );
    }

    private Integer idEmpresa(Usuario usuario) {
        return usuario == null || usuario.getEmpresa() == null
                ? SIN_EMPRESA
                : usuario.getEmpresa().getId();
    }

    private long contarRol(List<Usuario> usuarios, String rol) {
        return usuarios.stream()
                .filter(u -> rol.equalsIgnoreCase(u.getRol()))
                .count();
    }

    private long contarEstado(List<Asignacion> asignaciones, Asignacion.Estado estado) {
        return asignaciones.stream()
                .filter(a -> a.getEstado() == estado)
                .count();
    }
}
