package com.sah.controller.api;

import com.sah.dto.EmpresaRequestDTO;
import com.sah.dto.EmpresaResponseDTO;
import com.sah.dto.EstadoRequestDTO;
import com.sah.model.Empresa;
import com.sah.service.EmpresaService;

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
 * API REST de empresas (solo administrador del sistema).
 *
 * GET /api/empresas, GET /api/empresas/{id}, POST /api/empresas,
 * PUT /api/empresas/{id}, PATCH /api/empresas/{id}/estado.
 * No hay DELETE (405): las empresas tienen usuarios asociados.
 */
@RestController
@RequestMapping(value = "/api/empresas", produces = MediaType.APPLICATION_JSON_VALUE)
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public List<EmpresaResponseDTO> listar() {
        return empresaService.listarEmpresas().stream()
                .map(EmpresaResponseDTO::desde)
                .toList();
    }

    @GetMapping("/{id}")
    public EmpresaResponseDTO buscar(@PathVariable Integer id) {
        return EmpresaResponseDTO.desde(empresaService.buscarPorId(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmpresaResponseDTO> crear(
            @Valid @RequestBody EmpresaRequestDTO datos) {

        Empresa creada = empresaService.registrarEmpresa(datos.aEntidad());

        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.getId())
                .toUri();

        return ResponseEntity.created(ubicacion)
                .body(EmpresaResponseDTO.desde(creada));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EmpresaResponseDTO actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody EmpresaRequestDTO datos) {

        return EmpresaResponseDTO.desde(
                empresaService.actualizarEmpresa(id, datos.aEntidad())
        );
    }

    @PatchMapping(value = "/{id}/estado", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EmpresaResponseDTO cambiarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody EstadoRequestDTO datos) {

        return EmpresaResponseDTO.desde(
                empresaService.establecerEstado(
                        id, Empresa.EstadoEmpresa.valueOf(datos.estado()))
        );
    }
}
