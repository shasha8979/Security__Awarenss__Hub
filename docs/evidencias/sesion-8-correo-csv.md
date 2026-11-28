# Evidencia — Sesión 8: envío de correos y carga masiva por CSV

**Proyecto:** Security Awareness Hub (SAH) — SENA ADSO
**Fecha de la prueba:** 2026-11-27
**Rama:** `Andres`

## 1. Qué se construyó

| Pieza | Archivo | Responsabilidad |
|---|---|---|
| Carga masiva | `service/CargaUsuariosCsvService.java` | Lee el CSV, valida cada fila, guarda en bloques de 100 y publica un evento por usuario creado |
| Fila del CSV | `dto/FilaUsuarioCsvDTO.java` | Bean Validation (`@NotBlank`, `@Email`, `@Size`), las mismas reglas que `UsuarioRequestDTO` |
| Resultado | `dto/ResultadoCargaDTO.java`, `dto/ErrorFilaDTO.java` | Resumen: filas leídas, creadas, rechazadas y motivo por línea |
| Endpoint | `controller/api/UsuarioController.java` | `POST /api/usuarios/carga-csv` (multipart, campo `archivo`) — solo ADMIN |
| Interfaz | `controller/web/UsuarioWebController.java`, `templates/usuarios/lista.html` | Formulario de carga (multipart con token CSRF) en la página de usuarios. Hasta el 2026-11-27 era una vista Vaadin |
| Correo | `notificacion/CorreoService.java` | Envía con `JavaMailSender`; configuración SMTP por variables de entorno |
| Eventos | `notificacion/UsuarioRegistradoEvento.java`, `AsignacionCreadaEvento.java` | Datos del correo (la clave temporal no aparece en `toString()`) |
| Oyente | `notificacion/NotificacionCorreoListener.java` | `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` |

### Formato del archivo

```
nombres;apellidos;correo;nit_empresa;rol
Laura;Martínez;laura@empresa.com;900123456-7;empleado
Carlos;"Ruiz; Díaz";carlos@empresa.com;900123456-7;
```

- UTF-8 (se acepta el BOM que agrega Excel). Separador `;` o `,`, detectado en la cabecera.
- `rol` es opcional (por defecto `empleado`). Solo `empleado` o `admin_empresa`: **un CSV no puede crear administradores del sistema**.
- Máximo 1000 filas y 2 MB por archivo.
- Ejemplo listo para probar: [`api/usuarios-ejemplo.csv`](api/usuarios-ejemplo.csv) (3 filas válidas y 5 con error).

### Reglas

1. Primero se valida todo el archivo y después se guarda.
2. Las filas válidas se registran aunque otras tengan error. Cada error indica la **línea** y el motivo: campo vacío, correo con formato no válido, correo repetido en el archivo, correo ya registrado, rol no permitido, NIT inexistente o empresa inactiva.
3. Si el archivo en sí no sirve (sin extensión `.csv`, vacío, sin columnas obligatorias o con más de 1000 filas), se responde **400** y no se guarda nada.
4. Cada usuario recibe una **contraseña temporal aleatoria** (12 caracteres, `SecureRandom`). Se guarda cifrada con BCrypt y solo viaja en claro dentro del correo de bienvenida.

## 2. Decisiones de rendimiento (ítem 2 de la lista de chequeo)

| Decisión | Motivo |
|---|---|
| Guardar en bloques de 100 (`saveAll` + `flush` + `clear`) | El contexto de persistencia no crece con el tamaño del archivo |
| Una sola consulta para los correos existentes (`buscarCorreosRegistrados`) | Evita 1000 consultas `existsByCorreo` |
| Empresas en caché por NIT durante la carga | Una consulta por empresa, no una por fila |
| BCrypt en paralelo dentro de cada bloque | BCrypt tarda unos 0,1 s por clave a propósito; en paralelo se aprovechan los núcleos |
| Correos con `@Async` después del commit | Un SMTP lento no hace esperar al administrador, y no se avisa de usuarios que no quedaron guardados |

> **Limitación conocida:** con `GenerationType.IDENTITY`, Hibernate no agrupa los `INSERT`
> en lotes JDBC, porque necesita el id de cada fila. `hibernate.jdbc.batch_size` queda
> configurado, pero la mejora real viene de los bloques y de BCrypt en paralelo. Todavía no
> se ha medido el tiempo de una carga de 1000 filas (ver `docs/calidad/hallazgos-y-riesgos.md`).

## 3. Configuración del correo

Todo llega por variables de entorno o por el archivo local `backend/spring-boot/.env` (ver `.env.example`):

| Variable | Uso | Valor por defecto |
|---|---|---|
| `MAIL_ENABLED` | `true` envía; `false` solo registra en el log que se omitió | `false` |
| `MAIL_HOST` / `MAIL_PORT` | Servidor SMTP | `localhost` / `587` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Cuenta SMTP (en Gmail, una *contraseña de aplicación*) | vacío |
| `MAIL_FROM` | Remitente | `no-reply@sah.local` |

Si el servidor SMTP falla, la operación principal **no** se revierte: se registra un `WARN` (sin el cuerpo del correo, que contiene la clave) y se continúa.

## 4. Pruebas automáticas

Comando: `.\mvnw.cmd clean test` → **49 pruebas, 0 fallos** (2026-11-27).

| Clase | Tipo | Pruebas | Qué demuestra |
|---|---|---|---|
| `CargaUsuariosCsvServiceTest` | Unitaria (Mockito) | 5 | Carga válida, 7 tipos de error con su línea, 250 filas = 3 bloques con 1 consulta de correos, archivos no válidos, BOM |
| `CorreoServiceTest` | Integración (GreenMail SMTP) | 4 | El correo llega con asunto, destinatario y clave; deshabilitado no envía; SMTP caído no lanza excepción |
| `CargaCsvIntegracionTest` | Integración de extremo a extremo (MockMvc + MariaDB + GreenMail) | 4 | HTTP → seguridad → servicio → MariaDB → commit → correo. **La clave que llega por correo coincide con el hash BCrypt guardado**. 401, 403, 400 y 415 |
| `AsignacionServiceTest` | Unitaria | 4 | Reglas del avance y evento de correo al asignar |
| `DatosInicialesTest` | Unitaria | 4 | Admin semilla (Sesión 6): no duplica, avisa sin variables, crea con las variables, un error no detiene el arranque |
| `AsignacionIntegracionTest` | Integración (MockMvc + MariaDB) | 3 | `GET /api/asignaciones` filtrado por rol; `PATCH …/avance` 200/400/401/403/404 |

Las pruebas de integración crean datos con los dominios `@csv.sah-pruebas.com` y
`@avance.sah-pruebas.com` y los borran al terminar. Se verificó en MariaDB que quedan 0 registros.

## 5. Pruebas manuales (Postman / script)

Carpeta **"5 Carga masiva CSV (Sesión 8)"** de `api/SAH-API.postman_collection.json`, o el script:

```bash
bash docs/evidencias/api/probar-api-csv.sh 'CONTRASEÑA_ADMIN'
```

| # | Petición | Cuerpo | Esperado | Obtenido (2026-11-27) |
|---|---|---|---|---|
| 1 | `POST /api/usuarios/carga-csv` (admin) | `archivo=usuarios-ejemplo.csv` | 200 con resumen | **200** ✔ — `totalFilas 8`, `creados 0`, `rechazados 8`: los 3 válidos ya se habían creado en la prueba A1 del §6, así que ahora salen como "Ya existe un usuario con ese correo." |
| 2 | Igual, sin credenciales | `archivo=usuarios-ejemplo.csv` | 401 | **401** ✔ |
| 3 | Igual, clave incorrecta | `archivo=usuarios-ejemplo.csv` | 401 | **401** ✔ |
| 4 | Igual, estudiante inactivo | `archivo=usuarios-ejemplo.csv` | 401 | **401** ✔ (curl) |
| 5 | Archivo `.txt` (admin) | `archivo=usuarios.txt` | 400 | **400** ✔ "El archivo debe tener extensión .csv." |
| 6 | Archivo vacío (admin) | `archivo=vacio.csv` | 400 | **400** ✔ "El archivo está vacío o no tiene la línea de cabecera." |
| 7 | Cabecera sin columnas (admin) | `nombres;apellidos` | 400 | **400** ✔ "Faltan columnas en la cabecera: correo, nit_empresa…" |
| 8 | Sin campo `archivo` (admin) | `otro=…` | 400 | **400** ✔ |
| 9 | JSON en lugar de multipart (admin) | `{}` | 415 | **415** ✔ "El contenido debe enviarse como multipart/form-data." |
| 10 | Archivo de 3,9 MB (admin) | 60 000 filas | 413 | **413** ✔ "El archivo supera el tamaño máximo permitido (2 MB)"; 0 usuarios creados (curl) |

Resultado del script `probar-api-csv.sh` (2026-11-27, aplicación en marcha con `.\mvnw.cmd spring-boot:run`):
**8/8 pruebas correctas** (filas 1–3 y 5–9). Las filas 4 y 10 se probaron con `curl`.

El script verifica primero la clave del administrador. Si responde 401, se detiene con un mensaje
claro en lugar de mostrar fallas falsas (ver H12 en `docs/calidad/hallazgos-y-riesgos.md`).

## 6. Pruebas de aceptación en el navegador (2026-11-27)

Las ejecutó el equipo con la interfaz Vaadin (antes de pasar a Thymeleaf, D18); los resultados se verificaron en MariaDB. Los mismos flujos se prueban ahora en `WebIntegracionTest`.

| # | Rol | Pasos | Esperado | Obtenido |
|---|---|---|---|---|
| A1 | Administrador | Usuarios → Carga masiva → subir `usuarios-ejemplo.csv` | "Filas leídas: 8 · Registrados: 3 · Rechazados: 5" y tabla de errores en las líneas 5 a 9 | Igual al esperado ✔. En BD: laura, carlos (`empleado`) y marta (`admin_empresa`), activos, clave `$2a$` |
| A2 | Administrador | Asignaciones → asignar "Fundamentos de Ciberseguridad" a Laura | Asignación en estado pendiente | ✔ (asignación id 9) |
| A3 | Estudiante (Laura) | Mis capacitaciones → **Iniciar** → **Marcar como completada** | Estado "Completada" | ✔ En BD: `asignaciones.id=9` en `completado` |
| A4 | Administrador | Reportes | El avance se refleja | ✔ "Fundamentos de Ciberseguridad" 50 %, SecureCorp 100 %, completado general 33 % (1 de 3) |

Hallazgo de la prueba A1: el componente de carga mostraba sus textos en inglés ("Upload File...", "Drop file here").
Se tradujo con `UploadI18N` (H11 en `docs/calidad/hallazgos-y-riesgos.md`).

## 7. Pendiente de verificar

- Envío real por Gmail u otro SMTP (`MAIL_ENABLED=true`): probado solo con GreenMail.
