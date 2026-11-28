# Ejercicio "Gestor de Tareas": qué es y cómo se aplicó en SAH

Archivo original: `Gestión_Tareas.docx` (versión en Markdown prevista en
`docs/guias/ejercicio-gestor-tareas-thymeleaf-login.md`, que aún no está en el repositorio).

## Qué es

Es un **ejercicio complementario**, no una sesión numerada. Continúa el proyecto de ejemplo
`com.ejemplo.tareas` (ya con MySQL y JPA) y trae dos cosas:

1. **CRUD con vistas HTML (Thymeleaf), sin JSON.** Un `@Controller` devuelve plantillas; los
   formularios se envían por POST con `@ModelAttribute` y los mensajes viajan con
   `RedirectAttributes`. Además agrega el campo opcional `descripcion`.
2. **Inicio de sesión con formulario (`formLogin`).** Los usuarios están en una tabla `usuarios`
   de MySQL con contraseña BCrypt y rol `ADMIN` o `USER`, y se leen con un `UserDetailsService`
   propio. Tiene `/login`, `/logout`, sesión por cookie y CSRF activado. Solo ADMIN elimina, y eso
   se bloquea en el servidor (403).

## Diferencia con las guías 4 a 7

| Tema | Guías 4–7 | Este ejercicio |
|---|---|---|
| Tipo de aplicación | API REST con JSON (`@RestController`) | HTML renderizado en el servidor (`@Controller` + Thymeleaf) |
| Autenticación | HTTP Basic, sin sesión | Formulario de login con sesión |
| Usuarios | En memoria (guía 7) | En MySQL con `UserDetailsService` |
| CSRF | Deshabilitado (API) | Habilitado |
| Pruebas | Postman | Navegador |

## Decisión: resuelta — "Ambas"

La pregunta pendiente era si SAH debía ser API REST, páginas del servidor o ambas.
**El proyecto implementa ambas**, como sugiere el propio ejercicio, con una diferencia: la
interfaz se hizo con **Vaadin**, no con Thymeleaf (decisión D1 en `migracion-decisiones.md`).

| Lo que propone el ejercicio | Cómo quedó en SAH | Archivo |
|---|---|---|
| Dos `SecurityFilterChain` con `@Order` | `@Order(1)` para `/api/**`: HTTP Basic, `STATELESS`, CSRF deshabilitado. `@Order(2)` para la interfaz: formulario de login, sesión y CSRF | `security/SecurityConfig.java` |
| `UserDetailsService` que lee `usuarios` de MySQL | Lee por correo, rechaza cuentas inactivas y roles no válidos | `service/UsuarioDetailsService.java` |
| Hashes `$2y$` del PHP | Aceptados por `BCryptPasswordEncoder` (verificado el 2026-11-27 con un hash generado por PHP) | — |
| `DataInitializer` con `CommandLineRunner` | `DatosIniciales`: crea el admin solo si no hay ninguno activo, con `ADMIN_CORREO`/`ADMIN_PASSWORD` del `.env` | `config/DatosIniciales.java` |
| Solo ADMIN elimina (403 en el servidor) | `@RolesAllowed` en cada vista y `hasRole` en la API; no hay `DELETE` (se desactiva, D8) | Vistas y `SecurityConfig` |
| Mensajes flash | Notificaciones de Vaadin (`Notification`) | Vistas |

## Lo que NO se copió (choca con las reglas de las guías)

| Práctica del ejercicio | En SAH |
|---|---|
| Controlador que usa el repositorio directamente | Controlador o vista → `XxxService` → repositorio. `MisCapacitacionesView` se corrigió (hallazgo H4) |
| Entidad enlazada sin DTO ni validación | DTO `XxxRequestDTO` con `@Valid` en la API. Las vistas validan a través del servicio |
| Credenciales en `application.properties` y claves fijas (`admin123`) | Variables de entorno y `.env` ignorado por Git (D10). `DatosIniciales` no trae ninguna clave en el código |
| `deleteById` sin verificar | No hay borrado: primero 404 y luego desactivar |
| `spring-boot-starter-web` (Spring Boot 3) | SAH usa Spring Boot 4.1.3 con Vaadin 25 |

## Ítems de la Lista de Chequeo relacionados

- **Ítem 6** (autenticación y autorización): dos cadenas de seguridad, roles y 401/403 probados.
- **Ítem 1** (interfaz): resuelto con Vaadin.
