package com.sah.controller.web;

import com.sah.dto.EmpresaRequestDTO;
import com.sah.exception.RecursoNoEncontradoException;
import com.sah.model.Empresa;
import com.sah.service.EmpresaService;

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

/** Página de empresas (equivale a empresas.php). Reglas en EmpresaService. */
@Controller
@RequestMapping("/empresas")
public class EmpresaWebController {

    private final EmpresaService empresaService;

    public EmpresaWebController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "todos") String estado,
            Model model) {

        model.addAttribute("empresas", empresaService.buscarEmpresas(search, estado));
        model.addAttribute("search", search);
        model.addAttribute("estado", estado);
        return "empresas/lista";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        return formulario(model, EmpresaRequestDTO.vacio(), null, Map.of());
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes flash) {
        try {
            return formulario(model, EmpresaRequestDTO.desde(empresaService.buscarPorId(id)), id, Map.of());
        } catch (RecursoNoEncontradoException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/empresas";
        }
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) Integer id,
            @Valid @ModelAttribute("empresa") EmpresaRequestDTO datos,
            BindingResult validacion,
            Model model,
            RedirectAttributes flash) {

        if (validacion.hasErrors()) {
            return formulario(model, datos, id, ErroresFormulario.de(validacion));
        }

        try {
            if (id == null) {
                empresaService.registrarEmpresa(datos.aEntidad());
                flash.addFlashAttribute("mensaje", "Empresa registrada correctamente.");
            } else {
                empresaService.actualizarEmpresa(id, datos.aEntidad());
                flash.addFlashAttribute("mensaje", "Empresa actualizada correctamente.");
            }
        } catch (IllegalArgumentException ex) {
            return formulario(model, datos, id, ErroresFormulario.general(ex.getMessage()));
        }

        return "redirect:/empresas";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Empresa.EstadoEmpresa estado,
            RedirectAttributes flash) {

        try {
            empresaService.establecerEstado(id, estado);
            flash.addFlashAttribute("mensaje", estado == Empresa.EstadoEmpresa.activo
                    ? "Empresa activada." : "Empresa desactivada.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/empresas";
    }

    private String formulario(Model model, EmpresaRequestDTO empresa, Integer id, Map<String, String> errores) {
        model.addAttribute("empresa", empresa);
        model.addAttribute("id", id);
        model.addAttribute("errores", errores);
        model.addAttribute("tipos", Empresa.TipoEmpresa.values());
        model.addAttribute("titulo", id == null ? "Nueva empresa" : "Editar empresa");
        return "empresas/formulario";
    }
}
