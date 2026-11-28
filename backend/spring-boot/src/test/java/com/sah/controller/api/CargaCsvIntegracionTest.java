package com.sah.controller.api;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sah.model.Empresa;
import com.sah.model.Usuario;
import com.sah.repository.EmpresaRepository;
import com.sah.repository.UsuarioRepository;

import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integracion de la carga masiva (Sesion 8), de extremo a extremo:
 * peticion HTTP -> Spring Security -> controlador -> servicio -> MariaDB
 * -> commit -> evento -> correo SMTP (GreenMail en el puerto 3025).
 *
 * Usa la base de datos de desarrollo: crea una empresa y usuarios con el
 * dominio csv.sah-pruebas.com y los borra al terminar cada prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "sah.correo.habilitado=true",
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=3025",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false"
})
class CargaCsvIntegracionTest {

    private static final String DOMINIO = "@csv.sah-pruebas.com";
    private static final String RUTA = "/api/usuarios/carga-csv";

    @RegisterExtension
    static GreenMailExtension servidorSmtp = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Empresa empresa;
    private String sufijo;

    @BeforeEach
    void crearEmpresa() {
        sufijo = Long.toString(System.currentTimeMillis(), 36);

        empresa = new Empresa();
        empresa.setNombre("Empresa prueba CSV");
        empresa.setNit("CSV-" + sufijo);
        empresa.setCorreo("contacto" + DOMINIO);
        empresa.setTipo(Empresa.TipoEmpresa.privada);
        empresa.setEstado(Empresa.EstadoEmpresa.activo);
        empresa = empresaRepository.save(empresa);
    }

    @AfterEach
    void limpiar() {
        List<Usuario> creados = usuarioRepository.findAll().stream()
                .filter(u -> u.getCorreo().endsWith(DOMINIO))
                .toList();
        usuarioRepository.deleteAll(creados);
        empresaRepository.delete(empresa);
    }

    // Credenciales en cada peticion, como hace HTTP Basic (API sin sesion)
    private static final RequestPostProcessor ADMIN =
            user("admin@sah.com").roles("ADMIN");
    private static final RequestPostProcessor ESTUDIANTE =
            user("estudiante" + DOMINIO).roles("EMPLEADO");

    private MockMultipartFile archivo(String nombre, String contenido) {
        return new MockMultipartFile("archivo", nombre, "text/csv",
                contenido.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Admin carga CSV: 200, guarda en MariaDB con BCrypt y envía la clave por correo")
    void cargaCompleta() throws Exception {
        String ana = "ana." + sufijo + DOMINIO;
        String luis = "luis." + sufijo + DOMINIO;

        String csv = "nombres;apellidos;correo;nit_empresa;rol\n"
                + "Ana;Gómez;" + ana + ";" + empresa.getNit() + ";\n"
                + "Luis;Pérez;" + luis + ";" + empresa.getNit() + ";admin_empresa\n"
                + "Ana;Repetida;" + ana + ";" + empresa.getNit() + ";\n";

        mvc.perform(multipart(RUTA).file(archivo("usuarios.csv", csv)).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFilas").value(3))
                .andExpect(jsonPath("$.creados").value(2))
                .andExpect(jsonPath("$.rechazados").value(1))
                .andExpect(jsonPath("$.errores[0].fila").value(4))
                .andExpect(jsonPath("$.errores[0].mensaje")
                        .value("El correo está repetido en el archivo."));

        // Persistencia real en MariaDB
        Usuario guardada = usuarioRepository.findByCorreo(ana).orElseThrow();
        assertEquals("empleado", guardada.getRol());
        assertEquals("activo", guardada.getEstado());
        assertTrue(guardada.getPassword().startsWith("$2a$"));
        assertEquals(60, guardada.getPassword().length());

        // Correos enviados despues del commit, en segundo plano
        assertTrue(servidorSmtp.waitForIncomingEmail(10_000, 2));
        MimeMessage[] recibidos = servidorSmtp.getReceivedMessages();
        assertEquals(2, recibidos.length);

        MimeMessage correoAna = recibidos[0].getAllRecipients()[0].toString().equals(ana)
                ? recibidos[0] : recibidos[1];

        // La clave que llego por correo es la que quedo cifrada en la BD
        // getContent() decodifica el cuerpo (viaja en quoted-printable)
        Matcher clave = Pattern.compile("Contraseña temporal: (\\S+)")
                .matcher((String) correoAna.getContent());
        assertTrue(clave.find());
        assertTrue(passwordEncoder.matches(clave.group(1), guardada.getPassword()));
    }

    @Test
    @DisplayName("Sin credenciales -> 401")
    void sinCredenciales() throws Exception {
        mvc.perform(multipart(RUTA).file(archivo("usuarios.csv", "nombres\n")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Estudiante -> 403")
    void estudianteNoPuede() throws Exception {
        mvc.perform(multipart(RUTA).file(archivo("usuarios.csv", "nombres\n")).with(ESTUDIANTE))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Archivo no válido -> 400; sin archivo -> 400; JSON -> 415")
    void peticionesInvalidas() throws Exception {
        mvc.perform(multipart(RUTA).file(archivo("usuarios.txt", "x")).with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El archivo debe tener extensión .csv."));

        mvc.perform(multipart(RUTA).file(archivo("usuarios.csv", "nombres;apellidos\nA;B\n"))
                        .with(ADMIN))
                .andExpect(status().isBadRequest());

        mvc.perform(multipart(RUTA).with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("Debes adjuntar el archivo en el campo 'archivo'."));

        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}").with(ADMIN))
                .andExpect(status().isUnsupportedMediaType());
    }
}
