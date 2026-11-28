# Hallazgos, acciones correctivas y matriz de riesgos

Evidencia de los ítems 8 y 9 de la Lista de Chequeo 5:
verificación → hallazgo → acción correctiva o preventiva → verificación de la corrección.

## 1. Registro de hallazgos

| # | Fecha | Cómo se detectó | Hallazgo | Severidad | Acción | Verificación | Estado |
|---|---|---|---|---|---|---|---|
| H1 | 2026-11-27 | Revisión de código contra la guía 7 | `application.properties` se conectaba como `root` con la clave escrita en el archivo | Alta | Variables `DB_USER`/`DB_PASSWORD`, archivo `.env` en `.gitignore`, usuario `sah_app` con permisos mínimos | `clean test` pasa conectado como `sah_app`; `SHOW GRANTS` muestra solo `SELECT, INSERT, UPDATE, DELETE` | Cerrado |
| H2 | 2026-11-27 | Revisión de configuración | Log de Spring Security en `DEBUG/TRACE`: registra detalles de cada autenticación | Media | Nivel `INFO` y `show-sql=false` | Arranque sin trazas de autenticación | Cerrado |
| H3 | 2026-11-27 | Error al crear el usuario de BD (`Index for table 'db' is corrupt`) | Tabla del sistema `mysql.db` de XAMPP dañada (cierre abrupto de MariaDB) | Media | `REPAIR TABLE mysql.db` | `CHECK TABLE mysql.db` → `OK` | Cerrado |
| H4 | 2026-11-27 | Revisión de capas (guía 5) | `MisCapacitacionesView` consultaba los repositorios directamente, saltándose el servicio | Media | La vista usa `AsignacionService` | Compila; `AsignacionIntegracionTest` prueba el mismo servicio | Cerrado |
| H5 | 2026-11-27 | Prueba funcional del rol estudiante | El estudiante no podía registrar avance, así que los reportes siempre mostraban 0 % | Alta | `AsignacionService.registrarAvance`, botones en la vista y `PATCH /api/asignaciones/{id}/avance` | 7 pruebas automáticas y prueba en navegador (estudiante Laura: Iniciar → Completada; Reportes 50 %) | Cerrado |
| H6 | 2026-11-27 | `CargaCsvIntegracionTest` | Sin `spring.mail.default-encoding`, las tildes y la ñ de los correos podían llegar mal | Baja | `spring.mail.default-encoding=UTF-8` | El correo se decodifica y la clave coincide con el hash | Cerrado |
| H7 | 2026-11-27 | Revisión del manejador de errores | El 415 siempre decía "application/json", también en la ruta multipart | Baja | El mensaje usa el tipo que acepta la ruta | `CargaCsvIntegracionTest` (415) | Cerrado |
| H8 | 2026-11-27 | `CargaCsvIntegracionTest` falló con 401 | `@WithMockUser` solo autentica la primera petición en una API `STATELESS` (problema de la prueba, no de la app) | Baja | Autenticar cada petición con `.with(user(...))`, como hace HTTP Basic | 4/4 pruebas | Cerrado |
| H9 | 2026-11-27 | Revisión de `AsignacionService` | `crearAsignacion` usa `IllegalStateException` para permisos y duplicados; si llegara a la API respondería 500 | Baja | Preventiva: el endpoint nuevo usa excepciones propias (`AccesoDenegadoException` → 403). Pendiente cambiar `crearAsignacion` cuando se exponga por REST | — | Abierto |
| H10 | 2026-11-27 | Prueba de aceptación: el administrador no podía iniciar sesión | La contraseña guardada para `admin@sah.com` (hash `$2y$` del PHP) no coincidía con la que conocía el equipo. Se descartó un fallo de la app: login correcto e incorrecto probados con un usuario temporal, y Spring acepta hashes `$2y$` | Media | `database/restablecer_clave.php` (solo consola; responde 404 desde el navegador) | El administrador inició sesión con la clave nueva | Cerrado |
| H11 | 2026-11-27 | Prueba de aceptación A1 | El componente de carga CSV mostraba textos en inglés ("Upload File...", "Drop file here") | Baja | Traducción con `UploadI18N` | Verificado en el navegador: "Seleccionar archivo CSV..." / "o arrástralo aquí" | Cerrado |
| H12 | 2026-11-27 | Ejecución de `probar-api-csv.sh` | Con una clave de admin errónea, el script mostraba 6 fallas 401 que parecían errores de la API. La causa fue usar una clave distinta (con un carácter de más) de la que se guardó al restablecerla | Baja | El script valida primero la clave (`GET /api/cursos`) y, si no sirve, se detiene con instrucciones | Clave errónea: mensaje claro y salida 1. Clave correcta: 8/8 | Cerrado |

## 2. Matriz de riesgos

Probabilidad e impacto: 1 = bajo, 2 = medio, 3 = alto. Nivel = P × I.

| # | Riesgo | P | I | Nivel | Tratamiento | Responsable |
|---|---|---|---|---|---|---|
| R1 | Subir credenciales al repositorio | 2 | 3 | 6 | `.env` en `.gitignore`; solo se sube `.env.example`; revisar `git status` antes de cada commit | Equipo |
| R2 | Pérdida o daño de la BD local (XAMPP) | 2 | 3 | 6 | Scripts SQL versionados y repetibles; exportar copia antes de cada migración; `ddl-auto=validate` | Equipo |
| R3 | La contraseña temporal del CSV queda en el buzón y nunca se cambia | 2 | 2 | 4 | Pendiente: obligar a cambiarla en el primer inicio de sesión | Desarrollo |
| R4 | Carga CSV grande y lenta (BCrypt) | 2 | 2 | 4 | Límite de 1000 filas y 2 MB, bloques de 100, BCrypt en paralelo. Pendiente: medir con 1000 filas | Desarrollo |
| R5 | Servidor SMTP caído o cuenta bloqueada | 2 | 1 | 2 | El fallo se registra sin revertir la operación; `MAIL_ENABLED=false` en desarrollo | Desarrollo |
| R6 | Correos tomados como *phishing* (la plataforma enseña a desconfiar de correos con claves) | 2 | 2 | 4 | Remitente institucional (`MAIL_FROM`), texto sin enlaces y aviso de no compartir la clave | Equipo |
| R7 | Diferencias entre las guías y el proyecto (Vaadin, `validate`, roles) cuestionadas en la evaluación | 2 | 2 | 4 | Justificación en `docs/migracion-decisiones.md` (D1–D17) | Equipo |
| R8 | Retraso por depender de una sola persona para el despliegue o la BD | 2 | 2 | 4 | README con los pasos de arranque; todos los integrantes ejecutan `clean test` | Scrum Master |

## 3. Lista de verificación antes de cada entrega

- [ ] `.\mvnw.cmd clean test` sin fallos (anotar el número de pruebas)
- [ ] Scripts `probar-api-*.sh` ejecutados y resultados anotados en las evidencias
- [ ] `git status` sin `.env` ni credenciales
- [ ] Hallazgos nuevos registrados en la tabla 1
- [ ] Lista de Chequeo 5 recorrida (`docs/lista-chequeo-5.md`)
