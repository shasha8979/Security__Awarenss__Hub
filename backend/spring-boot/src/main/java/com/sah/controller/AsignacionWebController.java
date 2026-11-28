package com.sah.controller;

import com.sah.dto.AsignacionRequestDTO;
import com.sah.model.Asignacion;
import com.sah.service.AsignacionService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Página de asignaciones (equivale a asignaciones.php, que en PHP
 * nunca se implementó). La usan el administrador (todas) y el
 * administrador de empresa (solo sus estudiantes): AsignacionService
 * decide qué ve y qué puede asignar cada rol.
 */
@Controller
@RequestMapping("/asignaciones")
public class AsignacionWebController {

    private final AsignacionService asignacionService;

    public AsignacionWebController(AsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    @GetMapping
    public String listar(
            @RequestParam(required = false) Asignacion.Estado estado,
            @RequestParam(required = false) Asignacion.Tipo tipo,
            Model model) {

        return pagina(model, estado, tipo, Map.of());
    }

    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute AsignacionRequestDTO datos,
            BindingResult validacion,
            Model model,
            RedirectAttributes flash) {

        if (validacion.hasErrors()) {
            return pagina(model, null, null, ErroresFormulario.de(validacion));
        }

        try {
            asignacionService.crearAsignacion(
                    datos.usuarioId(), datos.cursoSegunTipo(), datos.evaluacionSegunTipo(), datos.tipo());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return pagina(model, null, null, ErroresFormulario.general(ex.getMessage()));
        }

        flash.addFlashAttribute("mensaje",
                "Asignación creada. El estudiante recibirá un aviso por correo.");
        return "redirect:/asignaciones";
    }

    private String pagina(
            Model model,
            Asignacion.Estado estado,
            Asignacion.Tipo tipo,
            Map<String, String> errores) {

        model.addAttribute("asignaciones", asignacionService.listarAsignaciones(estado, tipo));
        model.addAttribute("estudiantes", asignacionService.estudiantesAsignables());
        model.addAttribute("cursos", asignacionService.cursosAsignables());
        model.addAttribute("evaluaciones", asignacionService.evaluacionesAsignables());
        model.addAttribute("estado", estado);
        model.addAttribute("tipo", tipo);
        model.addAttribute("errores", errores);
        return "asignaciones/lista";
    }
}
