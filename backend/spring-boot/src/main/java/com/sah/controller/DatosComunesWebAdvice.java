package com.sah.controller;

import com.sah.dto.UsuarioSesionDTO;
import com.sah.service.UsuarioService;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Agrega a todas las páginas web el usuario que inició sesión ("sesion"),
 * que usan la barra lateral y la barra superior (fragments/layout.html).
 *
 * Solo aplica a los controladores de páginas, no a la API REST.
 */
@ControllerAdvice(assignableTypes = {
        PanelWebController.class,
        UsuarioWebController.class,
        EmpresaWebController.class,
        CursoWebController.class,
        EvaluacionWebController.class,
        AsignacionWebController.class,
        ReporteWebController.class,
        SeguimientoWebController.class,
        MisCapacitacionesWebController.class
})
public class DatosComunesWebAdvice {

    private final UsuarioService usuarioService;

    public DatosComunesWebAdvice(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @ModelAttribute("sesion")
    public UsuarioSesionDTO sesion(Authentication autenticacion) {
        if (autenticacion == null || autenticacion instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return UsuarioSesionDTO.desde(usuarioService.buscarPorCorreo(autenticacion.getName()));
    }
}
