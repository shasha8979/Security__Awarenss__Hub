# Evidencia — Sesión 6: Persistencia con MySQL/MariaDB y Spring Data JPA

**Proyecto:** Security Awareness Hub (SAH) — SENA ADSO
**Fecha de la prueba:** 2026-11-26
**Rama:** `Andres`

## 1. Configuración de la conexión

Archivo: `backend/spring-boot/src/main/resources/application.properties`

| Propiedad | Valor |
|---|---|
| `spring.datasource.url` | `jdbc:mariadb://localhost:3306/security_awareness_hub` |
| Motor | MariaDB 10.4.32 (XAMPP) |
| `spring.jpa.hibernate.ddl-auto` | `validate` |

Con `validate`, Hibernate **no crea ni borra tablas**: al arrancar compara cada entidad
con la tabla real y detiene la aplicación si no coinciden. Así se protegen los datos.

## 2. Entidades y repositorios

| Entidad (`com.sah.model`) | Tabla | Repositorio (`com.sah.repository`) | Relaciones |
|---|---|---|---|
| `Empresa` | `empresas` | `EmpresaRepository` | — |
| `Usuario` | `usuarios` | `UsuarioRepository` | `@ManyToOne` → `Empresa` |
| `Curso` | `cursos` | `CursoRepository` | — |
| `Evaluacion` | `evaluaciones` | `EvaluacionRepository` | `curso_id` (FK en BD) |
| `Asignacion` | `asignaciones` | `AsignacionRepository` | `@ManyToOne` → `Usuario`, `Curso`, `Evaluacion` |

Consultas derivadas utilizadas, por ejemplo:
`findByCorreo`, `findByEmpresa_IdAndRol`, `findByUsuario_Empresa_Id`,
`existsByUsuario_IdAndCurso_Id`, `existsByTituloIgnoreCase`.
Las relaciones se cargan con `@EntityGraph` para evitar consultas N+1.

## 3. Alineación del esquema (migración 001)

El script original `database/security_awareness_hub.sql` no coincidía con las entidades
(faltaban `usuarios.empresa_id`, los roles `admin_empresa`/`empleado` y la tabla
`asignaciones`), por lo que la aplicación no arrancaba
(`Schema validation: missing table [asignaciones]`).

Se creó `database/migracion_001_roles_empresas_asignaciones.sql`, que:

- No borra datos (sin `DROP`) y puede ejecutarse varias veces.
- Crea la tabla `asignaciones` con integridad en la base de datos:
  - `UNIQUE (usuario_id, curso_id)` y `UNIQUE (usuario_id, evaluacion_id)`: sin duplicados.
  - `CHECK` de coherencia entre `tipo` y `curso_id` / `evaluacion_id`.
  - Claves foráneas `ON DELETE RESTRICT`: no se borra contenido con asignaciones.

Pruebas de la migración (en base de datos temporal, luego eliminada):

| Prueba | Resultado |
|---|---|
| Ejecutar la migración dos veces | Sin errores ni duplicados |
| Insertar asignación duplicada | Rechazada (`uq_asignaciones_usuario_curso`) |
| Asignación tipo `evaluacion` con `curso_id` | Rechazada (`chk_asignaciones_tipo`) |
| Borrar un curso con asignaciones | Rechazado (`fk_asignaciones_curso`) |
| Arrancar Spring con `validate` | Correcto |

## 4. Datos registrados desde la aplicación

Registros creados por el usuario desde la interfaz Vaadin:

| Tabla | Registros |
|---|---|
| empresas | 1 (KFC) |
| usuarios | 4 (1 admin, 1 admin_empresa, 2 estudiantes) |
| cursos | 2 |
| asignaciones | 2 (estado `pendiente`, fecha asignada automáticamente) |

Las contraseñas se almacenan cifradas con BCrypt (`$2a$10$…`, 60 caracteres);
ninguna se guarda en texto plano.

## 5. Prueba de persistencia tras reinicio

1. Se detuvo la aplicación (puerto 8080 liberado).
2. Con la aplicación detenida se consultó MariaDB: 1 empresa, 4 usuarios, 2 cursos, 2 asignaciones.
3. Se volvió a iniciar con `.\mvnw.cmd spring-boot:run`: el esquema validó y la aplicación arrancó.
4. Inicio de sesión del administrador tras el reinicio: correcto (redirección a `/`).
5. Los datos siguen disponibles y se muestran en los paneles y en Reportes.

**Resultado:** los datos persisten en MariaDB de forma independiente del ciclo de vida
de la aplicación.

## 6. Capturas sugeridas para la sustentación

- [ ] phpMyAdmin con las tablas `empresas`, `usuarios` y `asignaciones`.
- [ ] Pantalla de Reportes con los indicadores después del reinicio.
- [ ] Panel de Seguimiento del administrador de empresa mostrando a sus estudiantes.
- [ ] Consola de Spring Boot con `Started SecurityAwarenessHubApplication`.

## 7. Pendientes relacionados

- La tabla de **resultados de evaluaciones** (respuestas y notas) aún no existe.
- Las tablas `preguntas` y `opciones` existen en la BD pero no tienen entidad JPA todavía.
