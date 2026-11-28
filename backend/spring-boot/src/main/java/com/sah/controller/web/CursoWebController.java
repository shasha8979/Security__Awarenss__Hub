package com.sah.controller.web;

import com.sah.dto.CursoRequestDTO;
import com.sah.exception.RecursoNoEncontradoException;
import com.sah.model.Curso;
import com.sah.service.CursoService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/** Página de cursos (equivale a courses.php). Reglas en CursoService. */
@Controller
@RequestMapping("/cursos")
public class CursoWebController {

    private final CursoService cursoService;

    public CursoWebController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "todos") String estado,
            Model model) {

        model.addAttribute("cursos", cursoService.buscarCursos(search, estado));
        model.addAttribute("search", search);
        model.addAttribute("estado", estado);
        return "cursos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return formulario(model, CursoRequestDTO.vacio(), null, Map.of());
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes flash) {
        try {
            return formulario(model, CursoRequestDTO.desde(cursoService.buscarPorId(id)), id, Map.of());
        } catch (RecursoNoEncontradoException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cursos";
        }
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) Integer id,
            @Valid @ModelAttribute("curso") CursoRequestDTO datos,
            BindingResult validacion,
            Model model,
            RedirectAttributes flash) {

        if (validacion.hasErrors()) {
            return formulario(model, datos, id, ErroresFormulario.de(validacion));
        }

        try {
            if (id == null) {
                cursoService.registrarCurso(datos.aEntidad());
                flash.addFlashAttribute("mensaje", "Curso creado correctamente.");
            } else {
                cursoService.actualizarCurso(id, datos.aEntidad());
                flash.addFlashAttribute("mensaje", "Curso actualizado correctamente.");
            }
        } catch (IllegalArgumentException ex) {
            return formulario(model, datos, id, ErroresFormulario.general(ex.getMessage()));
        }

        return "redirect:/cursos";
    }

    /** Activar / desactivar (como "completar / reabrir" del ejercicio). */
    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            Curso curso = cursoService.cambiarEstado(id);
            flash.addFlashAttribute("mensaje", curso.getEstado() == Curso.EstadoCurso.activo
                    ? "Curso activado." : "Curso desactivado.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cursos";
    }

    private String formulario(Model model, CursoRequestDTO curso, Integer id, Map<String, String> errores) {
        model.addAttribute("curso", curso);
        model.addAttribute("id", id);
        model.addAttribute("errores", errores);
        model.addAttribute("niveles", Curso.NivelDificultad.values());
        model.addAttribute("titulo", id == null ? "Nuevo curso" : "Editar curso");
        return "cursos/formulario";
    }
}
