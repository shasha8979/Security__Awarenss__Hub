package com.sah.controller;

import com.sah.dto.EvaluacionRequestDTO;
import com.sah.dto.EvaluacionResponseDTO;
import com.sah.exception.RecursoNoEncontradoException;
import com.sah.model.Evaluacion;
import com.sah.service.CursoService;
import com.sah.service.EvaluacionService;

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

/** Página de evaluaciones (equivale a evaluaciones.php). Reglas en EvaluacionService. */
@Controller
@RequestMapping("/evaluaciones")
public class EvaluacionWebController {

    private final EvaluacionService evaluacionService;
    private final CursoService cursoService;

    public EvaluacionWebController(EvaluacionService evaluacionService, CursoService cursoService) {
        this.evaluacionService = evaluacionService;
        this.cursoService = cursoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Integer cursoId, Model model) {

        // Igual que la API: se muestra el título del curso de cada evaluación
        model.addAttribute("evaluaciones", evaluacionService.listarEvaluaciones(cursoId).stream()
                .map(e -> EvaluacionResponseDTO.desde(e, evaluacionService.tituloCurso(e.getCursoId())))
                .toList());
        model.addAttribute("cursos", cursoService.listarCursos());
        model.addAttribute("cursoId", cursoId);
        return "evaluaciones/lista";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        return formulario(model, EvaluacionRequestDTO.vacio(), null, Map.of());
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes flash) {
        try {
            return formulario(model, EvaluacionRequestDTO.desde(evaluacionService.buscarPorId(id)), id, Map.of());
        } catch (RecursoNoEncontradoException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/evaluaciones";
        }
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) Integer id,
            @Valid @ModelAttribute("evaluacion") EvaluacionRequestDTO datos,
            BindingResult validacion,
            Model model,
            RedirectAttributes flash) {

        if (validacion.hasErrors()) {
            return formulario(model, datos, id, ErroresFormulario.de(validacion));
        }

        try {
            if (id == null) {
                evaluacionService.registrarEvaluacion(datos.aEntidad());
                flash.addFlashAttribute("mensaje", "Evaluación creada correctamente.");
            } else {
                evaluacionService.actualizarEvaluacion(id, datos.aEntidad());
                flash.addFlashAttribute("mensaje", "Evaluación actualizada correctamente.");
            }
        } catch (IllegalArgumentException ex) {
            return formulario(model, datos, id, ErroresFormulario.general(ex.getMessage()));
        }

        return "redirect:/evaluaciones";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Evaluacion.EstadoEvaluacion estado,
            RedirectAttributes flash) {

        try {
            evaluacionService.establecerEstado(id, estado);
            flash.addFlashAttribute("mensaje", estado == Evaluacion.EstadoEvaluacion.activo
                    ? "Evaluación activada." : "Evaluación desactivada.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/evaluaciones";
    }

    private String formulario(Model model, EvaluacionRequestDTO evaluacion, Integer id, Map<String, String> errores) {
        model.addAttribute("evaluacion", evaluacion);
        model.addAttribute("id", id);
        model.addAttribute("errores", errores);
        model.addAttribute("cursos", cursoService.listarCursos());
        model.addAttribute("titulo", id == null ? "Nueva evaluación" : "Editar evaluación");
        return "evaluaciones/formulario";
    }
}
