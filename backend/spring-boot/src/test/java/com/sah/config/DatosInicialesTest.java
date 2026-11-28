package com.sah.config;

import com.sah.exception.DatoDuplicadoException;
import com.sah.model.Usuario;
import com.sah.service.UsuarioService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de los datos iniciales (administrador semilla). */
class DatosInicialesTest {

    private UsuarioService usuarioService;

    @BeforeEach
    void preparar() {
        usuarioService = mock(UsuarioService.class);
    }

    @Test
    @DisplayName("Si ya hay un administrador activo, no crea ninguno")
    void yaExisteAdmin() {
        when(usuarioService.existeAdministradorActivo()).thenReturn(true);

        new DatosIniciales(usuarioService, "nuevo@sah.com", "ClaveSegura1").run();

        verify(usuarioService, never()).registrarUsuario(any(), anyString(), any());
    }

    @Test
    @DisplayName("Sin administrador y sin variables de entorno: solo avisa")
    void sinVariables() {
        when(usuarioService.existeAdministradorActivo()).thenReturn(false);

        new DatosIniciales(usuarioService, "", "").run();

        verify(usuarioService, never()).registrarUsuario(any(), anyString(), any());
    }

    @Test
    @DisplayName("Sin administrador y con variables: lo crea con rol admin, activo y sin empresa")
    void creaAdmin() {
        when(usuarioService.existeAdministradorActivo()).thenReturn(false);

        new DatosIniciales(usuarioService, "nuevo@sah.com", "ClaveSegura1").run();

        ArgumentCaptor<Usuario> creado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioService).registrarUsuario(creado.capture(), eq("ClaveSegura1"), isNull());
        assertEquals("nuevo@sah.com", creado.getValue().getCorreo());
        assertEquals("admin", creado.getValue().getRol());
        assertEquals("activo", creado.getValue().getEstado());
    }

    @Test
    @DisplayName("Si el registro falla (correo repetido), no detiene la aplicación")
    void errorNoDetiene() {
        when(usuarioService.existeAdministradorActivo()).thenReturn(false);
        when(usuarioService.registrarUsuario(any(), anyString(), any()))
                .thenThrow(new DatoDuplicadoException("Ya existe un usuario con ese correo."));

        assertDoesNotThrow(() ->
                new DatosIniciales(usuarioService, "repetido@sah.com", "ClaveSegura1").run());
    }
}
