package com.sah.controller;

import com.sah.exception.AccesoDenegadoException;
import com.sah.model.Asignacion;
import com.sah.service.AsignacionService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capacitaciones del estudiante y registro de su avance
 * (pendiente -> en progreso -> completada; reglas en AsignacionService).
 */
@Controller
@RequestMapping("/mis-capacitaciones")
public class MisCapacitacionesWebController {

    private final AsignacionService asignacionService;

    public MisCapacitacionesWebController(AsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("asignaciones", asignacionService.listarAsignaciones(null, null));
        return "mis-capacitaciones";
    }

    @PostMapping("/{id}/avance")
    public String registrarAvance(
            @PathVariable Integer id,
            @RequestParam Asignacion.Estado estado,
            RedirectAttributes flash) {

        try {
            asignacionService.registrarAvance(id, estado);
            flash.addFlashAttribute("mensaje", estado == Asignacion.Estado.completado
                    ? "¡Capacitación completada!" : "Capacitación iniciada.");
        } catch (IllegalArgumentException | AccesoDenegadoException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/mis-capacitaciones";
    }
}
