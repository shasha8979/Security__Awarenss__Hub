package com.sah.service;

import com.sah.dto.ErrorFilaDTO;
import com.sah.dto.ResultadoCargaDTO;
import com.sah.model.Empresa;
import com.sah.model.Usuario;
import com.sah.notificacion.UsuarioRegistradoEvento;
import com.sah.repository.EmpresaRepository;
import com.sah.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de la carga masiva de usuarios por CSV (Sesion 8).
 * Repositorios, cifrado y eventos se simulan con Mockito; la validacion
 * usa el Validator real de Bean Validation.
 */
class CargaUsuariosCsvServiceTest {

    private static ValidatorFactory fabricaValidadores;

    private UsuarioRepository usuarioRepository;
    private EmpresaRepository empresaRepository;
    private ApplicationEventPublisher eventos;
    private CargaUsuariosCsvService servicio;

    @BeforeAll
    static void crearValidador() {
        fabricaValidadores = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void cerrarValidador() {
        fabricaValidadores.close();
    }

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        empresaRepository = mock(EmpresaRepository.class);
        eventos = mock(ApplicationEventPublisher.class);

        PasswordEncoder cifrador = mock(PasswordEncoder.class);
        when(cifrador.encode(anyString())).thenAnswer(i -> "{hash}" + i.getArgument(0));

        Validator validador = fabricaValidadores.getValidator();

        servicio = new CargaUsuariosCsvService(
                usuarioRepository, empresaRepository, cifrador,
                validador, eventos, mock(EntityManager.class));

        when(usuarioRepository.buscarCorreosRegistrados(anyCollection()))
                .thenReturn(Set.of());
        when(empresaRepository.findByNit(anyString())).thenReturn(Optional.empty());
        when(empresaRepository.findByNit("900123456-1"))
                .thenReturn(Optional.of(empresa(Empresa.EstadoEmpresa.activo)));
        when(empresaRepository.findByNit("800999000-2"))
                .thenReturn(Optional.of(empresa(Empresa.EstadoEmpresa.inactivo)));
    }

    private static Empresa empresa(Empresa.EstadoEmpresa estado) {
        Empresa empresa = new Empresa();
        empresa.setId(1);
        empresa.setNombre("Empresa de prueba");
        empresa.setEstado(estado);
        return empresa;
    }

    private static InputStream csv(String texto) {
        return new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8));
    }

    private ResultadoCargaDTO cargar(String texto) throws IOException {
        return servicio.cargar("usuarios.csv", csv(texto));
    }

    @Test
    @DisplayName("Filas válidas: se registran como empleados activos con clave cifrada")
    @SuppressWarnings("unchecked")
    void cargaValida() throws IOException {
        ResultadoCargaDTO resultado = cargar("""
                nombres;apellidos;correo;nit_empresa;rol
                Ana;Gómez;ana@empresa.com;900123456-1;
                Luis;"Pérez; Ríos";luis@empresa.com;900123456-1;admin_empresa
                """);

        assertEquals(2, resultado.totalFilas());
        assertEquals(2, resultado.creados());
        assertEquals(0, resultado.rechazados());

        ArgumentCaptor<List<Usuario>> guardados = ArgumentCaptor.forClass(List.class);
        verify(usuarioRepository).saveAll(guardados.capture());

        Usuario ana = guardados.getValue().get(0);
        assertEquals("empleado", ana.getRol());
        assertEquals("activo", ana.getEstado());
        assertTrue(ana.getPassword().startsWith("{hash}"));

        Usuario luis = guardados.getValue().get(1);
        assertEquals("Pérez; Ríos", luis.getApellidos());
        assertEquals("admin_empresa", luis.getRol());

        // Un correo de bienvenida por usuario, con la clave sin cifrar
        ArgumentCaptor<UsuarioRegistradoEvento> avisos =
                ArgumentCaptor.forClass(UsuarioRegistradoEvento.class);
        verify(eventos, times(2)).publishEvent(avisos.capture());
        String clave = avisos.getAllValues().get(0).claveTemporal();
        assertEquals(12, clave.length());
        assertEquals("{hash}" + clave, ana.getPassword());
        assertNotEquals(clave, avisos.getAllValues().get(1).claveTemporal());
    }

    @Test
    @DisplayName("Filas con error: se informan con su línea y las válidas se registran")
    void filasConErrores() throws IOException {
        when(usuarioRepository.buscarCorreosRegistrados(anyCollection()))
                .thenReturn(Set.of("existe@empresa.com"));

        ResultadoCargaDTO resultado = cargar("""
                nombres,apellidos,correo,nit_empresa,rol
                Ana,Gómez,ana@empresa.com,900123456-1,empleado
                ,Sin nombre,sinnombre@empresa.com,900123456-1,empleado
                Pedro,Mal,correo-invalido,900123456-1,empleado
                Ana,Repetida,ANA@empresa.com,900123456-1,empleado
                Eva,Existe,existe@empresa.com,900123456-1,empleado

                Raul,Admin,raul@empresa.com,900123456-1,admin
                Sara,Nit,sara@empresa.com,111,empleado
                Tom,Inactiva,tom@empresa.com,800999000-2,empleado
                """);

        assertEquals(8, resultado.totalFilas());
        assertEquals(1, resultado.creados());
        assertEquals(7, resultado.rechazados());

        List<Integer> lineas = resultado.errores().stream().map(ErrorFilaDTO::fila).toList();
        assertEquals(List.of(3, 4, 5, 6, 8, 9, 10), lineas);

        List<String> mensajes = resultado.errores().stream().map(ErrorFilaDTO::mensaje).toList();
        assertEquals("Los nombres son obligatorios", mensajes.get(0));
        assertEquals("El correo no tiene un formato válido", mensajes.get(1));
        assertEquals("El correo está repetido en el archivo.", mensajes.get(2));
        assertEquals("Ya existe un usuario con ese correo.", mensajes.get(3));
        assertEquals("El rol debe ser empleado o admin_empresa.", mensajes.get(4));
        assertEquals("No existe una empresa con el NIT 111.", mensajes.get(5));
        assertEquals("La empresa con NIT 800999000-2 está inactiva.", mensajes.get(6));
    }

    @Test
    @DisplayName("250 filas válidas se guardan en 3 bloques (100 + 100 + 50)")
    void guardaPorBloques() throws IOException {
        StringBuilder texto = new StringBuilder("nombres;apellidos;correo;nit_empresa\n");
        for (int i = 1; i <= 250; i++) {
            texto.append("Nombre").append(i).append(";Apellido;usuario")
                    .append(i).append("@empresa.com;900123456-1\n");
        }

        ResultadoCargaDTO resultado = cargar(texto.toString());

        assertEquals(250, resultado.creados());
        verify(usuarioRepository, times(3)).saveAll(anyList());
        // Una sola consulta de correos existentes y una de empresa (cache)
        verify(usuarioRepository, times(1)).buscarCorreosRegistrados(anyCollection());
        verify(empresaRepository, times(1)).findByNit("900123456-1");
    }

    @Test
    @DisplayName("Archivo no válido: extensión, vacío, sin columnas o demasiadas filas -> 400")
    void archivoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.cargar("usuarios.xlsx", csv("nombres;apellidos")));

        assertThrows(IllegalArgumentException.class, () -> cargar(""));

        IllegalArgumentException sinColumnas = assertThrows(
                IllegalArgumentException.class,
                () -> cargar("nombres;apellidos\nAna;Gómez\n"));
        assertTrue(sinColumnas.getMessage().contains("correo, nit_empresa"));

        assertThrows(IllegalArgumentException.class,
                () -> cargar("nombres;apellidos;correo;nit_empresa\n\n"));

        StringBuilder grande = new StringBuilder("nombres;apellidos;correo;nit_empresa\n");
        for (int i = 0; i <= CargaUsuariosCsvService.MAXIMO_FILAS; i++) {
            grande.append("A;B;c").append(i).append("@x.com;1\n");
        }
        assertThrows(IllegalArgumentException.class, () -> cargar(grande.toString()));

        verify(usuarioRepository, never()).saveAll(anyList());
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Acepta el BOM que agrega Excel al guardar CSV UTF-8")
    void aceptaBom() throws IOException {
        ResultadoCargaDTO resultado = cargar(
                "﻿nombres;apellidos;correo;nit_empresa\nAna;Gómez;ana@empresa.com;900123456-1\n");

        assertEquals(1, resultado.creados());
    }
}
