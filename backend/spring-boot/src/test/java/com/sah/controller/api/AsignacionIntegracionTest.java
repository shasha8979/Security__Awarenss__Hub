package com.sah.controller.api;

import com.sah.model.Asignacion;
import com.sah.model.Curso;
import com.sah.model.Empresa;
import com.sah.model.Usuario;
import com.sah.repository.AsignacionRepository;
import com.sah.repository.CursoRepository;
import com.sah.repository.EmpresaRepository;
import com.sah.repository.UsuarioRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integracion del avance del estudiante por la API REST,
 * con la base de datos real. Crea empresa, curso, dos estudiantes y una
 * asignacion; al terminar los borra en orden inverso (claves foraneas).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AsignacionIntegracionTest {

    private static final String DOMINIO = "@avance.sah-pruebas.com";

    @Autowired private MockMvc mvc;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CursoRepository cursoRepository;
    @Autowired private AsignacionRepository asignacionRepository;

    private Empresa empresa;
    private Curso curso;
    private Usuario ana;
    private Usuario luis;
    private Asignacion asignacion;

    private RequestPostProcessor comoAna;
    private RequestPostProcessor comoLuis;

    @BeforeEach
    void crearDatos() {
        String sufijo = Long.toString(System.currentTimeMillis(), 36);

        empresa = new Empresa();
        empresa.setNombre("Empresa prueba avance");
        empresa.setNit("AV-" + sufijo);
        empresa.setCorreo("contacto" + DOMINIO);
        empresa.setTipo(Empresa.TipoEmpresa.privada);
        empresa.setEstado(Empresa.EstadoEmpresa.activo);
        empresa = empresaRepository.save(empresa);

        curso = new Curso();
        curso.setTitulo("Curso prueba avance " + sufijo);
        curso.setDescripcion("Curso temporal de la prueba de integración.");
        curso.setNivelDificultad(Curso.NivelDificultad.basico);
        curso.setDuracion(1);
        curso.setNumeroModulos(1);
        curso.setPorcentajeAprobacion(70);
        curso.setEstado(Curso.EstadoCurso.activo);
        curso = cursoRepository.save(curso);

        ana = usuarioRepository.save(estudiante("ana." + sufijo + DOMINIO));
        luis = usuarioRepository.save(estudiante("luis." + sufijo + DOMINIO));

        asignacion = new Asignacion();
        asignacion.setUsuario(ana);
        asignacion.setCurso(curso);
        asignacion.setTipo(Asignacion.Tipo.curso);
        asignacion = asignacionRepository.save(asignacion);

        comoAna = user(ana.getCorreo()).roles("EMPLEADO");
        comoLuis = user(luis.getCorreo()).roles("EMPLEADO");
    }

    @AfterEach
    void borrarDatos() {
        asignacionRepository.delete(asignacion);
        usuarioRepository.delete(ana);
        usuarioRepository.delete(luis);
        cursoRepository.delete(curso);
        empresaRepository.delete(empresa);
    }

    private Usuario estudiante(String correo) {
        Usuario u = new Usuario();
        u.setNombres("Prueba");
        u.setApellidos("Avance");
        u.setCorreo(correo);
        u.setPassword("{noop}no-se-usa-para-iniciar-sesion");
        u.setRol("empleado");
        u.setEstado("activo");
        u.setEmpresa(empresa);
        return u;
    }

    private String ruta() {
        return "/api/asignaciones/" + asignacion.getId() + "/avance";
    }

    private static String estado(String valor) {
        return "{\"estado\":\"" + valor + "\"}";
    }

    @Test
    @DisplayName("GET /api/asignaciones: el estudiante solo ve las suyas")
    void listaPropia() throws Exception {
        mvc.perform(get("/api/asignaciones").with(comoAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value(curso.getTitulo()))
                .andExpect(jsonPath("$[0].estado").value("pendiente"));

        mvc.perform(get("/api/asignaciones").with(comoLuis))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("PATCH avance: 200 en orden, 400 si salta o retrocede, se guarda en MariaDB")
    void avance() throws Exception {
        mvc.perform(patch(ruta()).with(comoAna)
                        .contentType(MediaType.APPLICATION_JSON).content(estado("completado")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Primero debes iniciar la capacitación."));

        mvc.perform(patch(ruta()).with(comoAna)
                        .contentType(MediaType.APPLICATION_JSON).content(estado("progreso")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("progreso"));

        mvc.perform(patch(ruta()).with(comoAna)
                        .contentType(MediaType.APPLICATION_JSON).content(estado("pendiente")))
                .andExpect(status().isBadRequest());

        mvc.perform(patch(ruta()).with(comoAna)
                        .contentType(MediaType.APPLICATION_JSON).content(estado("completado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("completado"));

        assertEquals(Asignacion.Estado.completado,
                asignacionRepository.findById(asignacion.getId()).orElseThrow().getEstado());
    }

    @Test
    @DisplayName("PATCH avance: 404 ajena, 403 admin, 401 sin credenciales, 400 estado inválido")
    void permisosYErrores() throws Exception {
        mvc.perform(patch(ruta()).with(comoLuis)
                        .contentType(MediaType.APPLICATION_JSON).content(estado("progreso")))
                .andExpect(status().isNotFound());

        mvc.perform(patch(ruta()).with(user("admin@sah.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(estado("progreso")))
                .andExpect(status().isForbidden());

        mvc.perform(patch(ruta())
                        .contentType(MediaType.APPLICATION_JSON).content(estado("progreso")))
                .andExpect(status().isUnauthorized());

        mvc.perform(patch(ruta()).with(comoAna)
                        .contentType(MediaType.APPLICATION_JSON).content(estado("terminado")))
                .andExpect(status().isBadRequest());

        mvc.perform(patch(ruta()).with(comoAna)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.estado").value("El estado es obligatorio"));
    }
}
