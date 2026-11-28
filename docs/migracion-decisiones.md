# Migración PHP → Spring Boot: estado y decisiones

**Proyecto:** Security Awareness Hub (SAH) — SENA ADSO, 5.º trimestre
**Última actualización:** 2026-11-27 — rama `Andres`

## 1. Estado

| Sesión de la guía | Tema | Estado | Evidencia |
|---|---|---|---|
| 4 | CRUD REST y códigos HTTP | Implementado y probado | `docs/evidencias/api/` (scripts y Postman) |
| 5 | Capas: controlador, servicio, repositorio | Implementado y probado | Paquetes `controller`, `service`, `repository` |
| 6 | Persistencia JPA con MariaDB | Implementado y probado | `docs/evidencias/sesion-6-persistencia.md` |
| 7 | DTOs, validación, errores, Spring Security | Implementado y probado | `docs/evidencias/api/README.md` |
| 7 | Credenciales en variables de entorno, usuario de BD propio | Implementado y probado (2026-11-27) | `database/crear_usuario_app.sql`, `.env.example` |
| 8 | Envío de correos y carga masiva por CSV | Implementado y probado con pruebas automáticas (2026-11-27) | `docs/evidencias/sesion-8-correo-csv.md` |

| Módulo | API REST | Interfaz Vaadin | Pruebas automáticas |
|---|---|---|---|
| Cursos | Sí | Sí | `CursoServiceTest` |
| Empresas | Sí | Sí | Scripts y Postman |
| Usuarios (+ carga CSV) | Sí | Sí | `CargaUsuariosCsvServiceTest`, `CargaCsvIntegracionTest` |
| Evaluaciones | Sí | Sí | Scripts y Postman |
| Asignaciones y avance | `GET` y `PATCH …/avance` | Sí | `AsignacionServiceTest`, `AsignacionIntegracionTest` |
| Reportes | — (solo interfaz) | Sí | `ReporteServiceTest` |

**Pendientes funcionales**

- Preguntas y opciones de las evaluaciones: las tablas `preguntas` y `opciones` existen, pero no tienen entidad ni pantalla. Para guardar la calificación del estudiante hay que añadir una columna, así que antes se propondrá la migración.
- Exportación de reportes (PDF/Excel).
- El `frontend/` HTML+JS del PHP todavía no consume la API de Spring Boot. La interfaz en uso es Vaadin.

## 2. Hallazgos del código PHP original

- El SQL original tenía `usuarios`, `cursos`, `evaluaciones`, `preguntas`, `opciones` y `empresas`, pero **no** tenía tablas de asignaciones ni de resultados. `ReporteController.php` decía que el reporte de resultados "todavía no está implementado". La tabla `asignaciones` se creó con `database/migracion_001_roles_empresas_asignaciones.sql`. La de resultados (calificaciones) sigue pendiente.
- El admin semilla usaba BCrypt `$2y$12$`, y `BCryptPasswordEncoder` de Spring lo acepta (verificado el 2026-11-27). Los usuarios del PHP pueden entrar sin cambiar su contraseña.
- El PHP validaba en los controladores y respondía `{success, message, data}` en snake_case. En Spring las validaciones están en servicios y DTOs, el JSON va en camelCase y la respuesta es el objeto mismo. Los errores tienen un formato único (`ErrorRespuestaDTO`).
- `courses.php` guardaba el estado con `(int)` sobre `"activo"/"inactivo"`, lo que corrompía el `ENUM`. Ya estaba corregido en el MVC PHP y en Spring se maneja como enumeración.

Análisis del ejercicio complementario "Gestor de Tareas" (Thymeleaf + login): [`gestor-tareas-analisis.md`](gestor-tareas-analisis.md).

## 3. Decisiones que se apartan de las guías o del PHP

| # | Fecha | Decisión | Guía / PHP decía | Motivo |
|---|---|---|---|---|
| D1 | 2026-11 | Interfaz con **Vaadin** además de la API REST | Las guías solo hacen API REST | El producto se evalúa con interfaz (ítem 1 de la lista). Vaadin usa los mismos servicios que la API, así las reglas no se duplican |
| D2 | 2026-11 | Paquete base `com.sah`, organizado **por capa** (`controller`, `service`…) | `com.securityhub`, por módulo | Ya estaba construido así y probado. Renombrar todo no cambia el comportamiento y arriesga la entrega |
| D3 | 2026-11 | `ddl-auto=validate` | `update` (solo para aprender) | La BD tiene datos reales; `validate` impide que Hibernate altere tablas. Los cambios de esquema van en `database/migracion_*.sql` |
| D4 | 2026-11 | **MariaDB de XAMPP** | MySQL 8 en Docker | Es el motor del entorno del SENA y del PHP original; no hay Docker en el equipo. El SQL es compatible |
| D5 | 2026-11 | Ids `Integer` | `Long` desde la Sesión 6 | Las columnas son `INT` en el SQL original; con `validate`, `Long` exige cambiar el esquema |
| D6 | 2026-11 | Roles `admin`, `admin_empresa`, `empleado` (se muestra "Estudiante") | `USER` y `ADMIN` | Son los roles del PHP y de la BD. `admin_empresa` hace falta para el seguimiento por empresa |
| D7 | 2026-11 | Dos cadenas de seguridad: HTTP Basic sin sesión para `/api/**` y formulario de login para Vaadin | Solo HTTP Basic | La interfaz web necesita sesión y CSRF; la API no |
| D8 | 2026-11 | Sin `DELETE` (405); se desactiva con `PATCH /{id}/estado` | `DELETE` → 204 | Las claves foráneas `ON DELETE RESTRICT` protegen el historial de asignaciones |
| D9 | 2026-11 | Relaciones JPA (`@ManyToOne`) con `@EntityGraph` | La guía 6 no trae relaciones | SAH las necesita (usuario–empresa, asignación–curso). `@EntityGraph` evita consultas N+1 |
| D10 | 2026-11-27 | Usuario de BD `sah_app` (solo `SELECT/INSERT/UPDATE/DELETE`) y `.env` local importado con `spring.config.import` | Variables de entorno | Igual que la guía, más un `.env` ignorado por Git para no tener que exportar variables en cada terminal |
| D11 | 2026-11-27 | La carga CSV identifica la empresa por **NIT** y no admite el rol `admin` | — | El NIT es lo que conoce quien prepara el archivo; un archivo no debe poder crear administradores del sistema |
| D12 | 2026-11-27 | Contraseña temporal aleatoria enviada por correo | — | El CSV no debe traer contraseñas. Riesgo: falta obligar a cambiarla (R3 en `docs/calidad/hallazgos-y-riesgos.md`) |
| D13 | 2026-11-27 | Correos por eventos (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`) | Envío directo desde el servicio | No se avisa de datos revertidos, y un SMTP lento no bloquea la petición |
| D14 | 2026-11-27 | Pruebas automáticas JUnit 5 + Mockito + MockMvc + GreenMail | Solo Postman | Ítem 8 de la lista: pruebas funcionales y de integración documentadas y repetibles |
| D15 | 2026-11-27 | Avance del estudiante paso a paso (`pendiente → progreso → completado`) | El PHP no lo tenía | Sin avance, los reportes de la empresa siempre marcaban 0 % |
| D16 | 2026-11-27 | Datos iniciales con `CommandLineRunner` (`config/DatosIniciales`): crea el admin solo si no hay ninguno activo, con `ADMIN_CORREO`/`ADMIN_PASSWORD` | Guía 6 y ejercicio Gestor de Tareas: `DataInitializer` con claves fijas | Mismo propósito que `crear_admin.php`, sin contraseñas en el código ni duplicar el admin en cada arranque |
| D17 | 2026-11-27 | Decisión "API REST, páginas o ambas": **ambas**, con Vaadin en lugar de Thymeleaf | Ejercicio Gestor de Tareas (Thymeleaf) | Ver `gestor-tareas-analisis.md`. Las dos cadenas de seguridad que propone el ejercicio ya estaban hechas |

## 4. Cómo ejecutar el proyecto

```powershell
# 1. Base de datos (XAMPP encendido): importar en este orden
#    database/security_awareness_hub.sql
#    database/migracion_001_roles_empresas_asignaciones.sql
#    database/crear_usuario_app.sql   (cambiar la clave antes)
# 2. Credenciales locales
cd backend\spring-boot
copy .env.example .env      # y escribir DB_PASSWORD
# 3. Pruebas y arranque
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run   # http://localhost:8080
```
