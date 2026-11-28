# Ejercicio "Gestor de Tareas": qué es y cómo se aplicó en SAH

Archivo original: `Gestión_Tareas.docx` (CRUD con vistas Thymeleaf e inicio de sesión con Spring Security).

## Qué es

Es un **ejercicio complementario**, no una sesión numerada. Continúa el proyecto `com.ejemplo.tareas` y trae dos cosas:

1. **CRUD con vistas HTML (Thymeleaf), sin JSON.** Un `@Controller` devuelve plantillas (`tareas/lista.html`, `tareas/formulario.html`). Los formularios se envían por POST, el guardado usa `@ModelAttribute` y los mensajes viajan con `RedirectAttributes` (flash).
2. **Inicio de sesión con formulario (`formLogin`).** Los usuarios están en la tabla `usuarios` con contraseña BCrypt y se leen con un `UserDetailsService` propio. Hay `/login`, `/logout`, sesión por cookie y CSRF activado (Thymeleaf agrega el token con `th:action`). Las restricciones de rol se aplican en el servidor (403).

## Decisión: "Ambas" — aplicada el 2026-11-28

SAH tiene **API REST** (`/api/**`, HTTP Basic, sin sesión, para Postman) **y páginas Thymeleaf** (formulario de login, sesión, CSRF), con dos `SecurityFilterChain` y `@Order`, como sugiere el ejercicio.

Las páginas usan **el mismo HTML y CSS del frontend PHP** (`frontend/Interface_Administrador/css/*.css` y `frontend/authentication/css/login.css`). Maven copia esos archivos a `static/` al compilar, así que hay una sola fuente de estilos.

La interfaz Vaadin que había antes se retiró: generaba su propio HTML y no podía usar el CSS del frontend (ver D18 en `migracion-decisiones.md`).

## Estructura (mismo orden que el ejercicio)

```
src/main/java/com/sah/
├── SecurityAwarenessHubApplication.java
├── config/
│   ├── SecurityConfig.java          <- 2 cadenas: API (Basic) y páginas (formLogin)
│   └── DatosIniciales.java          <- equivale a DataInitializer
├── controller/
│   ├── LoginController.java         <- /login y redirección inicial según el rol
│   ├── PanelWebController.java      <- páginas (@Controller) ...
│   ├── UsuarioWebController.java
│   ├── EmpresaWebController.java, CursoWebController.java, EvaluacionWebController.java
│   ├── AsignacionWebController.java, ReporteWebController.java
│   ├── SeguimientoWebController.java, MisCapacitacionesWebController.java
│   ├── DatosComunesWebAdvice.java   <- usuario en sesión para el menú
│   └── CursoController.java ...     <- API REST (@RestController, /api/**)
├── model/        repository/        service/
├── dto/          exception/         notificacion/
src/main/resources/
├── application.properties
└── templates/
    ├── login.html                   <- HTML de frontend/authentication/login.php
    ├── error.html                   <- 403 / 404 / 500 con el estilo del panel
    ├── fragments/layout.html        <- partials/_header.php, _footer.php, _flash.php
    ├── panel.html, reportes.html, seguimiento.html, mis-capacitaciones.html
    └── usuarios/, empresas/, cursos/, evaluaciones/   (lista.html + formulario.html)
        asignaciones/lista.html
```

## Lo que se tomó del ejercicio

| Ejercicio | SAH | Archivo |
|---|---|---|
| `@Controller` + plantillas `lista.html` / `formulario.html` | Un `XxxWebController` y dos plantillas por módulo | `controller/`, `templates/` |
| `@ModelAttribute` + `RedirectAttributes` (flash) | Igual; el mensaje se muestra con las clases `admin-alert` del frontend | Controladores web |
| Guardar con id oculto: crea o actualiza | `POST /xxx/guardar` con `id` opcional | Controladores web |
| "Completar / reabrir" | `POST /xxx/{id}/estado` (activar / desactivar) | Controladores web |
| `formLogin`, `loginPage("/login")`, `logout` → `/login?logout` | Igual, y `defaultSuccessUrl("/")` lleva a cada rol a su panel | `config/SecurityConfig.java` |
| `UserDetailsService` que lee `usuarios` de MySQL | Lee por correo, rechaza cuentas inactivas; acepta hashes `$2y$` del PHP | `service/UsuarioDetailsService.java` |
| `DataInitializer` con `CommandLineRunner` | `DatosIniciales`: crea el admin solo si no hay ninguno activo | `config/DatosIniciales.java` |
| Reglas de rol en el servidor (403) | `/usuarios`, `/cursos`… solo ADMIN; `/asignaciones` ADMIN y ADMIN_EMPRESA; `/mis-capacitaciones` EMPLEADO | `config/SecurityConfig.java` |
| CSRF con `th:action` | Todos los formularios, incluido el de carga CSV (multipart) | Plantillas |
| `spring.thymeleaf.cache=false` | Igual | `application.properties` |
| Tabla "Resultados esperados" (sección 8) | Prueba automática por cada fila | `WebIntegracionTest` (12 pruebas) |

## Lo que NO se copió (choca con las reglas de las guías)

| Práctica del ejercicio | En SAH |
|---|---|
| El controlador usa el repositorio directamente | Controlador → `XxxService` → repositorio. Las vistas Vaadin anteriores también lo hacían; se corrigió (H13) |
| Se enlaza la **entidad** con `@ModelAttribute`, sin validación | Se enlaza el **DTO** (`XxxRequestDTO`) con `@Valid`: las mismas reglas que la API REST. Los errores se muestran junto a cada campo |
| Credenciales en `application.properties` y claves fijas (`admin123`) | Variables de entorno y `.env` ignorado por Git; `DatosIniciales` no tiene ninguna clave en el código |
| `deleteById` sin verificar | No hay borrado: se desactiva (claves foráneas `RESTRICT`). Editar un id inexistente muestra un mensaje |
| `<style>` dentro de cada plantilla | Hojas de estilo del frontend (`dashboard.css`, `usuarios.css`…) |

## Cómo probar (como en la sección 7 del ejercicio)

1. Abre `http://localhost:8080/usuarios` sin sesión: te redirige a `/login`.
2. Clave incorrecta: muestra "Correo o contraseña incorrectos".
3. Entra como administrador: llegas a `/panel`. Un estudiante llega a `/mis-capacitaciones` y un administrador de empresa a `/seguimiento`.
4. Como estudiante, escribe a mano `/usuarios`: responde **403** (restricción en el servidor).
5. "Cerrar sesión" lleva a `/login?logout` con el mensaje de sesión cerrada.

## Ítems de la Lista de Chequeo relacionados

- **Ítem 1** (interfaz): mismo diseño, CSS y comportamiento responsivo del frontend original.
- **Ítem 6** (autenticación y autorización): dos cadenas de seguridad, roles, CSRF y 401/403 probados.
