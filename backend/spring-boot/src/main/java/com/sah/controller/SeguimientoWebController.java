package com.sah.controller;

import com.sah.dto.UsuarioSesionDTO;
import com.sah.service.ReporteService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Avance de los estudiantes de la empresa del administrador de empresa. */
@Controller
public class SeguimientoWebController {

    private final ReporteService reporteService;

    public SeguimientoWebController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/seguimiento")
    public String seguimiento(@ModelAttribute("sesion") UsuarioSesionDTO sesion, Model model) {

        model.addAttribute("estudiantes", sesion.empresaId() == null
                ? List.of()
                : reporteService.generarAvancePorEstudiante(sesion.empresaId()));
        return "seguimiento";
    }
}
