package com.sah.controller.web;

import com.sah.service.ReporteService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Reportes del administrador (equivale a reportes.php y export_reporte.php). */
@Controller
@RequestMapping("/reportes")
public class ReporteWebController {

    private final ReporteService reporteService;

    public ReporteWebController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    public String reportes(Model model) {
        model.addAttribute("resumen", reporteService.generarResumen());
        model.addAttribute("cursos", reporteService.generarAvancePorCurso());
        model.addAttribute("empresas", reporteService.generarAvancePorEmpresa());
        return "reportes";
    }

    /** Descarga el reporte en CSV: /reportes/exportar?tipo=cursos|empresas */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam String tipo) {

        try {
            byte[] contenido = reporteService.exportarCsv(tipo).getBytes(StandardCharsets.UTF_8);
            String archivo = "reporte-" + tipo + "-" + LocalDate.now() + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo + "\"")
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(contenido);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(ex.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }
}
