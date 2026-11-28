package com.sah.service;

import com.sah.dto.ErrorFilaDTO;
import com.sah.dto.FilaUsuarioCsvDTO;
import com.sah.dto.ResultadoCargaDTO;
import com.sah.model.Empresa;
import com.sah.model.Usuario;
import com.sah.notificacion.UsuarioRegistradoEvento;
import com.sah.repository.EmpresaRepository;
import com.sah.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Carga masiva de usuarios desde un archivo CSV (Sesion 8).
 *
 * Formato (UTF-8, separador coma o punto y coma, primera linea = cabecera):
 * <pre>
 * nombres;apellidos;correo;nit_empresa;rol
 * Ana;Gómez;ana.gomez@empresa.com;900123456-1;empleado
 * </pre>
 * La columna rol es opcional (por defecto "empleado"). Solo se permiten
 * los roles empleado y admin_empresa: un CSV no puede crear administradores
 * del sistema.
 *
 * Proceso:
 * 1. Se valida el archivo completo (formato y reglas) antes de guardar.
 * 2. Las filas con error se informan con su numero de linea y se omiten.
 * 3. Las filas validas se guardan en bloques de TAMANO_BLOQUE: cada bloque
 *    se envia a la base de datos (flush) y se libera de memoria (clear).
 * 4. A cada usuario creado se le genera una contraseña temporal, que se
 *    guarda cifrada (BCrypt) y se envia por correo despues del commit.
 */
@Service
public class CargaUsuariosCsvService {

    public static final int TAMANO_BLOQUE = 100;
    public static final int MAXIMO_FILAS = 1000;

    private static final List<String> COLUMNAS_OBLIGATORIAS =
            List.of("nombres", "apellidos", "correo", "nit_empresa");

    private static final List<String> ROLES_PERMITIDOS =
            List.of("empleado", "admin_empresa");

    // Sin caracteres que se confunden al leer (0/O, 1/l/I)
    private static final String ALFABETO_CLAVE =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LONGITUD_CLAVE = 12;

    private final SecureRandom aleatorio = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final ApplicationEventPublisher eventos;
    private final EntityManager entityManager;

    public CargaUsuariosCsvService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            Validator validator,
            ApplicationEventPublisher eventos,
            EntityManager entityManager) {

        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.eventos = eventos;
        this.entityManager = entityManager;
    }

    @Transactional
    public ResultadoCargaDTO cargar(String nombreArchivo, InputStream contenido)
            throws IOException {

        if (nombreArchivo == null
                || !nombreArchivo.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("El archivo debe tener extensión .csv.");
        }

        List<String> lineas = leerLineas(contenido);

        if (lineas.isEmpty() || lineas.get(0).isBlank()) {
            throw new IllegalArgumentException(
                    "El archivo está vacío o no tiene la línea de cabecera.");
        }

        char separador = lineas.get(0).indexOf(';') >= 0 ? ';' : ',';
        Map<String, Integer> columnas = leerCabecera(lineas.get(0), separador);

        // Filas de datos con su numero de linea real (se omiten lineas vacias)
        List<Integer> numerosLinea = new ArrayList<>();
        for (int i = 1; i < lineas.size(); i++) {
            if (!lineas.get(i).isBlank()) {
                numerosLinea.add(i + 1);
            }
        }

        if (numerosLinea.isEmpty()) {
            throw new IllegalArgumentException("El archivo no tiene filas de datos.");
        }

        if (numerosLinea.size() > MAXIMO_FILAS) {
            throw new IllegalArgumentException(
                    "El archivo tiene " + numerosLinea.size()
                            + " filas; el máximo por carga es " + MAXIMO_FILAS + ".");
        }

        // 1. Convertir cada linea en una fila
        List<FilaUsuarioCsvDTO> filas = new ArrayList<>();
        for (int numero : numerosLinea) {
            filas.add(convertirFila(dividir(lineas.get(numero - 1), separador), columnas));
        }

        // Una sola consulta para saber que correos ya existen
        Set<String> correosRecibidos = new HashSet<>();
        for (FilaUsuarioCsvDTO fila : filas) {
            if (fila.correo() != null && !fila.correo().isBlank()) {
                correosRecibidos.add(fila.correo().toLowerCase(Locale.ROOT));
            }
        }
        Set<String> correosRegistrados = correosRecibidos.isEmpty()
                ? Set.of()
                : usuarioRepository.buscarCorreosRegistrados(correosRecibidos);

        // 2. Validar fila por fila
        List<ErrorFilaDTO> errores = new ArrayList<>();
        List<Usuario> validos = new ArrayList<>();
        Set<String> correosDelArchivo = new HashSet<>();
        Map<String, Optional<Empresa>> empresasPorNit = new HashMap<>();

        for (int i = 0; i < filas.size(); i++) {

            FilaUsuarioCsvDTO fila = filas.get(i);
            int numero = numerosLinea.get(i);

            String error = validarFila(
                    fila, correosRegistrados, correosDelArchivo, empresasPorNit);

            if (error != null) {
                errores.add(new ErrorFilaDTO(numero, fila.correo(), error));
                continue;
            }

            correosDelArchivo.add(fila.correo().toLowerCase(Locale.ROOT));
            validos.add(crearUsuario(fila, empresasPorNit.get(fila.nitEmpresa()).get()));
        }

        // 3. Guardar en bloques
        List<UsuarioRegistradoEvento> avisos = new ArrayList<>();

        for (int inicio = 0; inicio < validos.size(); inicio += TAMANO_BLOQUE) {

            List<Usuario> bloque = validos.subList(
                    inicio, Math.min(inicio + TAMANO_BLOQUE, validos.size()));

            // BCrypt es lento a proposito (~0,1 s por clave): se cifran
            // las claves del bloque en paralelo para no hacer esperar
            List<String> claves = bloque.stream().map(u -> generarClave()).toList();
            List<String> hashes = claves.parallelStream()
                    .map(passwordEncoder::encode)
                    .toList();

            for (int j = 0; j < bloque.size(); j++) {
                Usuario usuario = bloque.get(j);
                usuario.setPassword(hashes.get(j));
                avisos.add(new UsuarioRegistradoEvento(
                        usuario.getCorreo(), usuario.getNombres(), claves.get(j)));
            }

            usuarioRepository.saveAll(bloque);
            entityManager.flush();
            entityManager.clear();
        }

        // 4. Los correos salen despues del commit (NotificacionCorreoListener)
        avisos.forEach(eventos::publishEvent);

        errores.sort(Comparator.comparingInt(ErrorFilaDTO::fila));

        return new ResultadoCargaDTO(
                filas.size(), validos.size(), errores.size(), errores);
    }

    private String validarFila(
            FilaUsuarioCsvDTO fila,
            Set<String> correosRegistrados,
            Set<String> correosDelArchivo,
            Map<String, Optional<Empresa>> empresasPorNit) {

        Set<ConstraintViolation<FilaUsuarioCsvDTO>> violaciones = validator.validate(fila);
        if (!violaciones.isEmpty()) {
            return violaciones.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .findFirst()
                    .orElse("Fila no válida.");
        }

        if (!ROLES_PERMITIDOS.contains(fila.rol())) {
            return "El rol debe ser empleado o admin_empresa.";
        }

        String correo = fila.correo().toLowerCase(Locale.ROOT);

        if (correosDelArchivo.contains(correo)) {
            return "El correo está repetido en el archivo.";
        }

        if (correosRegistrados.contains(correo)) {
            return "Ya existe un usuario con ese correo.";
        }

        Optional<Empresa> empresa = empresasPorNit.computeIfAbsent(
                fila.nitEmpresa(), empresaRepository::findByNit);

        if (empresa.isEmpty()) {
            return "No existe una empresa con el NIT " + fila.nitEmpresa() + ".";
        }

        if (empresa.get().getEstado() != Empresa.EstadoEmpresa.activo) {
            return "La empresa con NIT " + fila.nitEmpresa() + " está inactiva.";
        }

        return null;
    }

    private Usuario crearUsuario(FilaUsuarioCsvDTO fila, Empresa empresa) {
        Usuario usuario = new Usuario();
        usuario.setNombres(fila.nombres());
        usuario.setApellidos(fila.apellidos());
        usuario.setCorreo(fila.correo());
        usuario.setRol(fila.rol());
        usuario.setEstado("activo");
        usuario.setEmpresa(empresa);
        return usuario;
    }

    private String generarClave() {
        StringBuilder clave = new StringBuilder(LONGITUD_CLAVE);
        for (int i = 0; i < LONGITUD_CLAVE; i++) {
            clave.append(ALFABETO_CLAVE.charAt(aleatorio.nextInt(ALFABETO_CLAVE.length())));
        }
        return clave.toString();
    }

    // ---------- Lectura del archivo ----------

    private List<String> leerLineas(InputStream contenido) throws IOException {
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(contenido, StandardCharsets.UTF_8))) {

            List<String> lineas = new ArrayList<>(lector.lines().toList());

            // Excel guarda "CSV UTF-8" con BOM al inicio
            if (!lineas.isEmpty() && lineas.get(0).startsWith("﻿")) {
                lineas.set(0, lineas.get(0).substring(1));
            }
            return lineas;
        }
    }

    private Map<String, Integer> leerCabecera(String linea, char separador) {

        List<String> nombres = dividir(linea, separador);
        Map<String, Integer> columnas = new HashMap<>();

        for (int i = 0; i < nombres.size(); i++) {
            columnas.put(nombres.get(i).toLowerCase(Locale.ROOT), i);
        }

        List<String> faltantes = COLUMNAS_OBLIGATORIAS.stream()
                .filter(columna -> !columnas.containsKey(columna))
                .toList();

        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Faltan columnas en la cabecera: " + String.join(", ", faltantes)
                            + ". Se esperaba: nombres, apellidos, correo, nit_empresa, rol.");
        }
        return columnas;
    }

    private FilaUsuarioCsvDTO convertirFila(List<String> valores, Map<String, Integer> columnas) {
        String rol = valor(valores, columnas.get("rol"));
        return new FilaUsuarioCsvDTO(
                valor(valores, columnas.get("nombres")),
                valor(valores, columnas.get("apellidos")),
                valor(valores, columnas.get("correo")),
                valor(valores, columnas.get("nit_empresa")),
                rol == null || rol.isBlank() ? "empleado" : rol.toLowerCase(Locale.ROOT)
        );
    }

    private String valor(List<String> valores, Integer indice) {
        if (indice == null || indice >= valores.size()) {
            return null;
        }
        String texto = valores.get(indice);
        return texto.isBlank() ? null : texto;
    }

    /**
     * Divide una linea respetando comillas dobles: "Gómez; Pérez" es un
     * solo valor y "" dentro de comillas representa una comilla.
     */
    static List<String> dividir(String linea, char separador) {

        List<String> valores = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);

            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (c == separador && !entreComillas) {
                valores.add(actual.toString().trim());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        valores.add(actual.toString().trim());
        return valores;
    }
}
