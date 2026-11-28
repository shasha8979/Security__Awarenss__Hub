package com.sah.controller;

import com.sah.service.ReporteService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Dashboard del administrador (equivale a panel.php). */
@Controller
public class PanelWebController {

    private final ReporteService reporteService;

    public PanelWebController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        model.addAttribute("resumen", reporteService.generarResumen());
        return "panel";
    }
}
