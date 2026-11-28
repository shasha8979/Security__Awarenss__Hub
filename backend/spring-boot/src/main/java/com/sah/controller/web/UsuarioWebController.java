package com.sah.controller.web;

import com.sah.dto.ResultadoCargaDTO;
import com.sah.dto.UsuarioRequestDTO;
import com.sah.exception.RecursoNoEncontradoException;
import com.sah.service.CargaUsuariosCsvService;
import com.sah.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Página de usuarios (equivale a usuarios.php), con el molde del
 * ejercicio Gestor de Tareas: lista, formulario, guardar y mensajes flash.
 *
 * Diferencias con el ejercicio (reglas de las guías 5 y 7):
 * el formulario se valida con UsuarioRequestDTO (@Valid) y todas las
 * reglas están en UsuarioService; el controlador no usa repositorios.
 * No se elimina: se desactiva (las asignaciones lo impiden).
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioWebController {

    private final UsuarioService usuarioService;
    private final CargaUsuariosCsvService cargaUsuariosCsvService;

    public UsuarioWebController(
            UsuarioService usuarioService,
            CargaUsuariosCsvService cargaUsuariosCsvService) {

        this.usuarioService = usuarioService;
        this.cargaUsuariosCsvService = cargaUsuariosCsvService;
    }

    // READ: lista con búsqueda
    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "todos") String estado,
            Model model) {

        model.addAttribute("usuarios", usuarioService.buscarUsuarios(search, estado));
        model.addAttribute("search", search);
        model.addAttribute("estado", estado);
        return "usuarios/lista";
    }

    // CREATE: formulario vacío
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return formulario(model, UsuarioRequestDTO.vacio(), null, Map.of());
    }

    // UPDATE: formulario con los datos actuales
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes flash) {
        try {
            return formulario(model, UsuarioRequestDTO.desde(usuarioService.buscarPorId(id)), id, Map.of());
        } catch (RecursoNoEncontradoException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/usuarios";
        }
    }

    // CREATE / UPDATE: el id oculto decide si registra o actualiza
    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) Integer id,
            @Valid @ModelAttribute("usuario") UsuarioRequestDTO datos,
            BindingResult validacion,
            Model model,
            RedirectAttributes flash) {

        if (validacion.hasErrors()) {
            return formulario(model, datos, id, ErroresFormulario.de(validacion));
        }

        try {
            if (id == null) {
                usuarioService.registrarUsuario(
                        datos.aEntidad("activo"), datos.password(), datos.empresaId());
                flash.addFlashAttribute("mensaje", "Usuario creado correctamente.");
            } else {
                String estadoActual = usuarioService.buscarPorId(id).getEstado();
                usuarioService.actualizarUsuario(
                        id, datos.aEntidad(estadoActual), datos.password(), datos.empresaId());
                flash.addFlashAttribute("mensaje", "Usuario actualizado correctamente.");
            }
        } catch (IllegalArgumentException ex) {
            return formulario(model, datos, id, ErroresFormulario.general(ex.getMessage()));
        }

        return "redirect:/usuarios";
    }

    // UPDATE rápido: activar / desactivar
    @PostMapping("/{id}/estado")
    public String cambiarEstado(
            @PathVariable Integer id,
            @RequestParam String estado,
            Authentication autenticacion,
            RedirectAttributes flash) {

        try {
            usuarioService.establecerEstado(id, estado, autenticacion.getName());
            flash.addFlashAttribute("mensaje",
                    "activo".equals(estado) ? "Usuario activado." : "Usuario desactivado.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/usuarios";
    }

    // Carga masiva por CSV (Sesión 8): mismo servicio que POST /api/usuarios/carga-csv
    @PostMapping("/carga-csv")
    public String cargarCsv(@RequestParam("archivo") MultipartFile archivo, RedirectAttributes flash) {

        try (InputStream contenido = archivo.getInputStream()) {
            ResultadoCargaDTO resultado =
                    cargaUsuariosCsvService.cargar(archivo.getOriginalFilename(), contenido);
            flash.addFlashAttribute("resultadoCarga", resultado);
            flash.addFlashAttribute("mensaje", resultado.creados() + " usuario(s) registrado(s) desde el CSV.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (IOException ex) {
            flash.addFlashAttribute("error", "No se pudo leer el archivo.");
        }
        return "redirect:/usuarios";
    }

    private String formulario(Model model, UsuarioRequestDTO usuario, Integer id, Map<String, String> errores) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("id", id);
        model.addAttribute("errores", errores);
        model.addAttribute("empresas", usuarioService.listarEmpresasActivas());
        model.addAttribute("titulo", id == null ? "Nuevo usuario" : "Editar usuario");
        return "usuarios/formulario";
    }
}
