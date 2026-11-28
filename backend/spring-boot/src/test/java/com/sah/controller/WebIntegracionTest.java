package com.sah.controller;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Pruebas de las páginas Thymeleaf (ejercicio Gestor de Tareas, sección 8
 * "Resultados esperados"), contra la base de datos real.
 *
 * Crea una empresa, un administrador de empresa, un estudiante con
 * contraseña conocida, un curso y una asignación; los borra al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebIntegracionTest {

    private static final String DOMINIO = "@web.sah-pruebas.com";
    private static final String CLAVE = "ClaveWeb-2026";

    @Autowired private MockMvc mvc;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CursoRepository cursoRepository;
    @Autowired private AsignacionRepository asignacionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Empresa empresa;
    private Usuario adminEmpresa;
    private Usuario estudiante;
    private Curso curso;
    private Asignacion asignacion;
    private String sufijo;

    private RequestPostProcessor comoAdmin;
    private RequestPostProcessor comoAdminEmpresa;
    private RequestPostProcessor comoEstudiante;

    @BeforeEach
    void crearDatos() {
        sufijo = Long.toString(System.currentTimeMillis(), 36);

        empresa = new Empresa();
        empresa.setNombre("Empresa prueba web");
        empresa.setNit("WEB-" + sufijo);
        empresa.setCorreo("contacto" + DOMINIO);
        empresa.setTipo(Empresa.TipoEmpresa.privada);
        empresa.setEstado(Empresa.EstadoEmpresa.activo);
        empresa = empresaRepository.save(empresa);

        adminEmpresa = usuarioRepository.save(usuario("jefe." + sufijo + DOMINIO, "admin_empresa"));
        estudiante = usuarioRepository.save(usuario("ana." + sufijo + DOMINIO, "empleado"));

        curso = new Curso();
        curso.setTitulo("Curso prueba web " + sufijo);
        curso.setDescripcion("Curso temporal de la prueba web.");
        curso.setNivelDificultad(Curso.NivelDificultad.basico);
        curso.setDuracion(2);
        curso.setNumeroModulos(1);
        curso.setPorcentajeAprobacion(70);
        curso.setEstado(Curso.EstadoCurso.activo);
        curso = cursoRepository.save(curso);

        asignacion = new Asignacion();
        asignacion.setUsuario(estudiante);
        asignacion.setCurso(curso);
        asignacion.setTipo(Asignacion.Tipo.curso);
        asignacion = asignacionRepository.save(asignacion);

        comoAdmin = user("admin@sah.com").roles("ADMIN");
        comoAdminEmpresa = user(adminEmpresa.getCorreo()).roles("ADMIN_EMPRESA");
        comoEstudiante = user(estudiante.getCorreo()).roles("EMPLEADO");
    }

    @AfterEach
    void borrarDatos() {
        asignacionRepository.findAll().stream()
                .filter(a -> a.getUsuario().getCorreo().endsWith(DOMINIO))
                .forEach(asignacionRepository::delete);
        usuarioRepository.findAll().stream()
                .filter(u -> u.getCorreo().endsWith(DOMINIO))
                .forEach(usuarioRepository::delete);
        cursoRepository.findAll().stream()
                .filter(c -> c.getTitulo().contains(sufijo))
                .forEach(cursoRepository::delete);
        empresaRepository.delete(empresa);
    }

    private Usuario usuario(String correo, String rol) {
        Usuario u = new Usuario();
        u.setNombres("Prueba");
        u.setApellidos("Web");
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode(CLAVE));
        u.setRol(rol);
        u.setEstado("activo");
        u.setEmpresa(empresa);
        return u;
    }

    // ---------- Resultados esperados del ejercicio ----------

    @Test
    @DisplayName("Sin sesión: /usuarios redirige a /login; el login y su CSS son públicos")
    void sinSesion() throws Exception {
        mvc.perform(get("/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bienvenido")))
                .andExpect(content().string(containsString("/auth/css/login.css")));

        mvc.perform(get("/admin/css/dashboard.css")).andExpect(status().isOk());
        mvc.perform(get("/auth/css/login.css")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login: credenciales incorrectas -> /login?error; correctas -> / y panel según el rol")
    void login() throws Exception {
        mvc.perform(formLogin("/login").user(estudiante.getCorreo()).password("incorrecta"))
                .andExpect(redirectedUrl("/login?error"));

        mvc.perform(formLogin("/login").user(estudiante.getCorreo()).password(CLAVE))
                .andExpect(redirectedUrl("/"));

        mvc.perform(get("/").with(comoAdmin)).andExpect(redirectedUrl("/panel"));
        mvc.perform(get("/").with(comoAdminEmpresa)).andExpect(redirectedUrl("/seguimiento"));
        mvc.perform(get("/").with(comoEstudiante)).andExpect(redirectedUrl("/mis-capacitaciones"));
    }

    @Test
    @DisplayName("Cerrar sesión -> /login?logout")
    void cerrarSesion() throws Exception {
        mvc.perform(logout()).andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    @DisplayName("Roles: el estudiante recibe 403 en páginas de administración, aunque escriba la URL")
    void permisos() throws Exception {
        mvc.perform(get("/usuarios").with(comoEstudiante)).andExpect(status().isForbidden());
        mvc.perform(get("/reportes").with(comoAdminEmpresa)).andExpect(status().isForbidden());
        mvc.perform(get("/mis-capacitaciones").with(comoAdmin)).andExpect(status().isForbidden());
        mvc.perform(post("/cursos/" + curso.getId() + "/estado").with(comoEstudiante).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF: un POST sin token se rechaza (403)")
    void csrfObligatorio() throws Exception {
        mvc.perform(post("/cursos/" + curso.getId() + "/estado").with(comoAdmin))
                .andExpect(status().isForbidden());
    }

    // ---------- Cada página se renderiza con los CSS del frontend ----------

    @Test
    @DisplayName("Administrador: todas sus páginas cargan (200) con el CSS del frontend")
    void paginasAdmin() throws Exception {
        String[][] paginas = {
                {"/panel", "Dashboard", null},
                {"/usuarios", "Usuarios registrados", "usuarios.css"},
                {"/usuarios/nuevo", "Nuevo usuario", "usuarios.css"},
                {"/usuarios/editar/" + estudiante.getId(), "Editar usuario", "usuarios.css"},
                {"/empresas", "Empresas registradas", "empresas.css"},
                {"/empresas/nueva", "Nueva empresa", "empresas.css"},
                {"/cursos", "Cursos disponibles", "courses_admin.css"},
                {"/cursos/editar/" + curso.getId(), "Editar curso", "courses_admin.css"},
                {"/evaluaciones", "Evaluaciones registradas", "evaluaciones.css"},
                {"/evaluaciones/nueva", "Nueva evaluación", "evaluaciones.css"},
                {"/asignaciones", "Asignar capacitación", "asignaciones.css"},
                {"/reportes", "Avance por curso", "reportes.css"},
        };

        for (String[] pagina : paginas) {
            var resultado = mvc.perform(get(pagina[0]).with(comoAdmin))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(pagina[1])))
                    .andExpect(content().string(containsString("/admin/css/dashboard.css")));
            if (pagina[2] != null) {
                resultado.andExpect(content().string(containsString("/admin/css/" + pagina[2])));
            }
        }
    }

    @Test
    @DisplayName("Administrador de empresa y estudiante: sus páginas cargan con sus datos")
    void paginasOtrosRoles() throws Exception {
        mvc.perform(get("/seguimiento").with(comoAdminEmpresa))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Avance por estudiante")))
                .andExpect(content().string(containsString(estudiante.getCorreo())));

        mvc.perform(get("/asignaciones").with(comoAdminEmpresa))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(estudiante.getCorreo())));

        mvc.perform(get("/mis-capacitaciones").with(comoEstudiante))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(curso.getTitulo())))
                .andExpect(content().string(containsString("Iniciar")));
    }

    // ---------- Formularios: validación (DTO + @Valid) y mensajes flash ----------

    @Test
    @DisplayName("Formulario con datos inválidos: vuelve al formulario con el error del campo")
    void formularioInvalido() throws Exception {
        mvc.perform(post("/cursos/guardar").with(comoAdmin).with(csrf())
                        .param("titulo", "")
                        .param("descripcion", "x")
                        .param("nivelDificultad", "basico")
                        .param("duracion", "1")
                        .param("numeroModulos", "1")
                        .param("porcentajeAprobacion", "150"))
                .andExpect(status().isOk())
                .andExpect(view().name("cursos/formulario"))
                .andExpect(content().string(containsString("El título es obligatorio")));
    }

    @Test
    @DisplayName("Editar y cambiar estado: redirige a la lista con mensaje flash y guarda en la BD")
    void editarYCambiarEstado() throws Exception {
        mvc.perform(post("/cursos/guardar").with(comoAdmin).with(csrf())
                        .param("id", curso.getId().toString())
                        .param("titulo", curso.getTitulo() + " editado")
                        .param("descripcion", "Descripción editada desde la página.")
                        .param("nivelDificultad", "intermedio")
                        .param("duracion", "4")
                        .param("numeroModulos", "2")
                        .param("porcentajeAprobacion", "80"))
                .andExpect(redirectedUrl("/cursos"))
                .andExpect(flash().attribute("mensaje", "Curso actualizado correctamente."));

        assertEquals(Curso.NivelDificultad.intermedio,
                cursoRepository.findById(curso.getId()).orElseThrow().getNivelDificultad());

        mvc.perform(post("/cursos/" + curso.getId() + "/estado").with(comoAdmin).with(csrf()))
                .andExpect(redirectedUrl("/cursos"))
                .andExpect(flash().attribute("mensaje", "Curso desactivado."));
    }

    @Test
    @DisplayName("Estudiante registra su avance desde la página")
    void avanceEstudiante() throws Exception {
        mvc.perform(post("/mis-capacitaciones/" + asignacion.getId() + "/avance")
                        .with(comoEstudiante).with(csrf()).param("estado", "progreso"))
                .andExpect(redirectedUrl("/mis-capacitaciones"))
                .andExpect(flash().attribute("mensaje", "Capacitación iniciada."));

        assertEquals(Asignacion.Estado.progreso,
                asignacionRepository.findById(asignacion.getId()).orElseThrow().getEstado());
    }

    @Test
    @DisplayName("Carga CSV desde la página (multipart con token CSRF)")
    void cargaCsvDesdePagina() throws Exception {
        String csv = "nombres;apellidos;correo;nit_empresa\n"
                + "Luis;Web;luis." + sufijo + DOMINIO + ";" + empresa.getNit() + "\n";

        mvc.perform(multipart("/usuarios/carga-csv")
                        .file(new MockMultipartFile("archivo", "usuarios.csv", "text/csv",
                                csv.getBytes(StandardCharsets.UTF_8)))
                        .with(comoAdmin).with(csrf()))
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("mensaje", "1 usuario(s) registrado(s) desde el CSV."));
    }

    @Test
    @DisplayName("Reportes: exportación CSV")
    void exportarCsv() throws Exception {
        mvc.perform(get("/reportes/exportar").param("tipo", "cursos").with(comoAdmin))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString(curso.getTitulo())));

        mvc.perform(get("/reportes/exportar").param("tipo", "otro").with(comoAdmin))
                .andExpect(status().isBadRequest());
    }
}
