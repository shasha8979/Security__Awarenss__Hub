package com.sah.controller.api;

import com.sah.dto.AsignacionResponseDTO;
import com.sah.dto.AvanceRequestDTO;
import com.sah.service.AsignacionService;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de asignaciones.
 *
 * | Método | Ruta                            | Quién            | Respuesta          |
 * |--------|---------------------------------|------------------|--------------------|
 * | GET    | /api/asignaciones               | cualquier rol    | 200 (según el rol) |
 * | PATCH  | /api/asignaciones/{id}/avance   | estudiante       | 200, 400, 403, 404 |
 *
 * GET devuelve todas (admin), las de su empresa (admin_empresa) o las
 * propias (estudiante). Las reglas están en AsignacionService.
 */
@RestController
@RequestMapping(value = "/api/asignaciones", produces = MediaType.APPLICATION_JSON_VALUE)
public class AsignacionController {

    private final AsignacionService asignacionService;

    public AsignacionController(AsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    @GetMapping
    public List<AsignacionResponseDTO> listar() {
        return asignacionService.listarAsignaciones().stream()
                .map(AsignacionResponseDTO::desde)
                .toList();
    }

    @PatchMapping(value = "/{id}/avance", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AsignacionResponseDTO registrarAvance(
            @PathVariable Integer id,
            @Valid @RequestBody AvanceRequestDTO datos) {

        return AsignacionResponseDTO.desde(
                asignacionService.registrarAvance(id, datos.estado()));
    }
}
