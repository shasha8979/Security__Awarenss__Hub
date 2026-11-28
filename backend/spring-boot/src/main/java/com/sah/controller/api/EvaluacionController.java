package com.sah.controller.api;

import com.sah.dto.EstadoRequestDTO;
import com.sah.dto.EvaluacionRequestDTO;
import com.sah.dto.EvaluacionResponseDTO;
import com.sah.model.Curso;
import com.sah.model.Evaluacion;
import com.sah.service.CursoService;
import com.sah.service.EvaluacionService;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API REST de evaluaciones.
 *
 * GET /api/evaluaciones[?cursoId=1], GET /api/evaluaciones/{id}
 * (cualquier usuario autenticado), POST, PUT y
 * PATCH /api/evaluaciones/{id}/estado (solo administrador).
 * No hay DELETE (405): tienen preguntas y asignaciones.
 */
@RestController
@RequestMapping(value = "/api/evaluaciones", produces = MediaType.APPLICATION_JSON_VALUE)
public class EvaluacionController {

    private final EvaluacionService evaluacionService;
    private final CursoService cursoService;

    public EvaluacionController(
            EvaluacionService evaluacionService,
            CursoService cursoService) {
        this.evaluacionService = evaluacionService;
        this.cursoService = cursoService;
    }

    @GetMapping
    public List<EvaluacionResponseDTO> listar(
            @RequestParam(required = false) Integer cursoId) {

        // Un solo acceso a cursos para todos los titulos (evita N+1)
        Map<Integer, String> titulos = cursoService.listarCursos().stream()
                .collect(Collectors.toMap(Curso::getId, Curso::getTitulo));

        return evaluacionService.listarEvaluaciones(cursoId).stream()
                .map(e -> EvaluacionResponseDTO.desde(e, titulos.get(e.getCursoId())))
                .toList();
    }

    @GetMapping("/{id}")
    public EvaluacionResponseDTO buscar(@PathVariable Integer id) {
        return respuesta(evaluacionService.buscarPorId(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvaluacionResponseDTO> crear(
            @Valid @RequestBody EvaluacionRequestDTO datos) {

        Evaluacion creada = evaluacionService.registrarEvaluacion(datos.aEntidad());

        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.getId())
                .toUri();

        return ResponseEntity.created(ubicacion).body(respuesta(creada));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EvaluacionResponseDTO actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody EvaluacionRequestDTO datos) {

        return respuesta(evaluacionService.actualizarEvaluacion(id, datos.aEntidad()));
    }

    @PatchMapping(value = "/{id}/estado", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EvaluacionResponseDTO cambiarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody EstadoRequestDTO datos) {

        return respuesta(evaluacionService.establecerEstado(
                id, Evaluacion.EstadoEvaluacion.valueOf(datos.estado())));
    }

    private EvaluacionResponseDTO respuesta(Evaluacion evaluacion) {
        return EvaluacionResponseDTO.desde(
                evaluacion, evaluacionService.tituloCurso(evaluacion.getCursoId()));
    }
}
