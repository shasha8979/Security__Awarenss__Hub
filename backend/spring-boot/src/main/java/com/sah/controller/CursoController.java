package com.sah.controller;

import com.sah.dto.CursoRequestDTO;
import com.sah.dto.CursoResponseDTO;
import com.sah.dto.EstadoCursoRequestDTO;
import com.sah.model.Curso;
import com.sah.service.CursoService;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST de cursos.
 *
 * | Metodo | Ruta                       | Respuesta            |
 * |--------|----------------------------|----------------------|
 * | GET    | /api/cursos                | 200 lista            |
 * | GET    | /api/cursos/{id}           | 200 / 404            |
 * | POST   | /api/cursos                | 201 + Location / 400 / 409 |
 * | PUT    | /api/cursos/{id}           | 200 / 400 / 404 / 409 |
 * | PATCH  | /api/cursos/{id}/estado    | 200 / 400 / 404      |
 *
 * No hay DELETE: los cursos tienen evaluaciones y asignaciones
 * relacionadas, por eso se desactivan (DELETE responde 405).
 *
 * El controlador solo traduce HTTP <-> DTO; las reglas de negocio
 * estan en CursoService y los errores en ManejadorGlobalErrores.
 */
@RestController
@RequestMapping(value = "/api/cursos", produces = MediaType.APPLICATION_JSON_VALUE)
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public List<CursoResponseDTO> listar() {
        return cursoService.listarCursos().stream()
                .map(CursoResponseDTO::desde)
                .toList();
    }

    @GetMapping("/{id}")
    public CursoResponseDTO buscar(@PathVariable Integer id) {
        return CursoResponseDTO.desde(cursoService.buscarPorId(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CursoResponseDTO> crear(
            @Valid @RequestBody CursoRequestDTO datos) {

        Curso creado = cursoService.registrarCurso(datos.aEntidad());

        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.getId())
                .toUri();

        return ResponseEntity.created(ubicacion)
                .body(CursoResponseDTO.desde(creado));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CursoResponseDTO actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CursoRequestDTO datos) {

        return CursoResponseDTO.desde(
                cursoService.actualizarCurso(id, datos.aEntidad())
        );
    }

    @PatchMapping(value = "/{id}/estado", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CursoResponseDTO cambiarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody EstadoCursoRequestDTO datos) {

        return CursoResponseDTO.desde(
                cursoService.establecerEstado(id, datos.estado())
        );
    }
}
