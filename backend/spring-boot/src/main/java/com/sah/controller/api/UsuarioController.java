package com.sah.controller.api;

import com.sah.dto.EstadoRequestDTO;
import com.sah.dto.ResultadoCargaDTO;
import com.sah.dto.UsuarioRequestDTO;
import com.sah.dto.UsuarioResponseDTO;
import com.sah.model.Usuario;
import com.sah.service.CargaUsuariosCsvService;
import com.sah.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * API REST de usuarios (solo administrador del sistema).
 *
 * GET /api/usuarios[?rol=empleado], GET /api/usuarios/{id},
 * POST /api/usuarios, PUT /api/usuarios/{id},
 * PATCH /api/usuarios/{id}/estado,
 * POST /api/usuarios/carga-csv (multipart, campo "archivo": carga masiva).
 * No hay DELETE (405): los usuarios tienen asignaciones; se desactivan.
 */
@RestController
@RequestMapping(value = "/api/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final CargaUsuariosCsvService cargaUsuariosCsvService;

    public UsuarioController(
            UsuarioService usuarioService,
            CargaUsuariosCsvService cargaUsuariosCsvService) {
        this.usuarioService = usuarioService;
        this.cargaUsuariosCsvService = cargaUsuariosCsvService;
    }

    @GetMapping
    public List<UsuarioResponseDTO> listar(
            @RequestParam(required = false) String rol) {

        return usuarioService.listarUsuarios().stream()
                .filter(usuario -> rol == null || rol.equalsIgnoreCase(usuario.getRol()))
                .map(UsuarioResponseDTO::desde)
                .toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO buscar(@PathVariable Integer id) {
        return UsuarioResponseDTO.desde(usuarioService.buscarPorId(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioResponseDTO> crear(
            @Valid @RequestBody UsuarioRequestDTO datos) {

        Usuario creado = usuarioService.registrarUsuario(
                datos.aEntidad("activo"),
                datos.password(),
                datos.empresaId()
        );

        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.getId())
                .toUri();

        return ResponseEntity.created(ubicacion)
                .body(UsuarioResponseDTO.desde(creado));
    }

    /**
     * Carga masiva por CSV. Responde 200 con el resumen: las filas válidas
     * se registran y las inválidas se devuelven en "errores" con su línea.
     * 400 si el archivo en sí no es válido (vacío, sin cabecera, no .csv).
     */
    @PostMapping(value = "/carga-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultadoCargaDTO cargarCsv(@RequestParam("archivo") MultipartFile archivo)
            throws IOException {

        try (InputStream contenido = archivo.getInputStream()) {
            return cargaUsuariosCsvService.cargar(archivo.getOriginalFilename(), contenido);
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UsuarioResponseDTO actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioRequestDTO datos) {

        // Si no se envia estado, se conserva el actual (404 si no existe)
        String estadoActual = usuarioService.buscarPorId(id).getEstado();

        return UsuarioResponseDTO.desde(
                usuarioService.actualizarUsuario(
                        id,
                        datos.aEntidad(estadoActual),
                        datos.password(),
                        datos.empresaId()
                )
        );
    }

    @PatchMapping(value = "/{id}/estado", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UsuarioResponseDTO cambiarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody EstadoRequestDTO datos,
            Authentication autenticacion) {

        return UsuarioResponseDTO.desde(
                usuarioService.establecerEstado(
                        id, datos.estado(), autenticacion.getName())
        );
    }
}
