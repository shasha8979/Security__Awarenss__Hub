# API REST — Security Awareness Hub

Evidencia de las guías 4 (API REST y códigos HTTP), 5 (capas), 7 (validación,
manejo de errores y seguridad) y 8 (carga masiva por CSV y correos).

## Endpoints

Base: `http://localhost:8080/api` — autenticación **HTTP Basic** (usuario = correo).

| Recurso | GET lista | GET {id} | POST | PUT {id} | PATCH {id}/estado | DELETE |
|---|---|---|---|---|---|---|
| `/cursos` | autenticado | autenticado | admin | admin | admin | 405 |
| `/evaluaciones` (`?cursoId=`) | autenticado | autenticado | admin | admin | admin | 405 |
| `/empresas` | admin | admin | admin | admin | admin | 405 |
| `/usuarios` (`?rol=`) | admin | admin | admin | admin | admin | 405 |

Rutas adicionales:

| Ruta | Método | Quién | Respuesta |
|---|---|---|---|
| `/usuarios/carga-csv` | POST (multipart, campo `archivo`) | admin | 200 con resumen (`totalFilas`, `creados`, `rechazados`, `errores` por línea); 400 si el archivo no es válido; 413 si pasa de 2 MB; 415 si no es multipart |
| `/asignaciones` | GET | autenticado | 200: todas (admin), las de su empresa (admin_empresa) o las propias (estudiante) |
| `/asignaciones/{id}/avance` | PATCH `{"estado":"progreso"}` | estudiante | 200; 400 si salta o retrocede de estado; 403 otro rol; 404 asignación ajena o inexistente |

Detalle de la Sesión 8: [`../sesion-8-correo-csv.md`](../sesion-8-correo-csv.md).

**DELETE responde 405 a propósito:** cursos, evaluaciones, empresas y usuarios
tienen registros relacionados (asignaciones, usuarios, preguntas) protegidos por
claves foráneas `ON DELETE RESTRICT`. En lugar de borrarse se desactivan con
`PATCH /{id}/estado` y cuerpo `{"estado":"inactivo"}`.

## Códigos HTTP

| Código | Cuándo |
|---|---|
| 200 | Consulta, actualización o cambio de estado correctos |
| 201 | Recurso creado (incluye cabecera `Location`) |
| 400 | Validación fallida (con errores por campo), JSON mal formado, valor de enumeración no válido, regla de negocio |
| 401 | Sin credenciales, credenciales incorrectas o usuario inactivo |
| 403 | El rol no tiene permiso (por ejemplo, un estudiante intenta ver usuarios) |
| 404 | El recurso o la ruta no existen |
| 405 | Método no permitido (DELETE) |
| 413 | Archivo CSV mayor de 2 MB |
| 409 | Dato único repetido (título de curso, NIT, correo, evaluación en el mismo curso) |
| 415 | `Content-Type` distinto de `application/json` |
| 500 | Error interno (se registra en el log; al cliente no se le muestran detalles) |

Todas las respuestas de error usan el mismo formato:

```json
{
  "fecha": "2026-11-26T01:58:11",
  "estado": 400,
  "error": "Bad Request",
  "mensaje": "Los datos enviados no son válidos.",
  "ruta": "/api/usuarios",
  "campos": { "correo": "El correo no tiene un formato válido" }
}
```

## Arquitectura

```
Controller (com.sah.controller)  → HTTP ⇄ DTO, @Valid, códigos de respuesta
Service    (com.sah.service)     → reglas de negocio (las mismas que usa la interfaz Vaadin)
Repository (com.sah.repository)  → Spring Data JPA → MariaDB
DTO        (com.sah.dto)         → entrada validada / salida sin datos sensibles
Exception  (com.sah.exception)   → ManejadorGlobalErrores (@RestControllerAdvice)
```

Protección de datos: el cliente no puede fijar `id`, `estado` ni fechas; las
contraseñas se cifran con BCrypt y **nunca** aparecen en las respuestas.

## Cómo ejecutar las pruebas

Con la aplicación en marcha, desde la raíz del repositorio en **Git Bash**:

```bash
bash docs/evidencias/api/probar-api-cursos.sh 'CONTRASEÑA_ADMIN'   # 21 pruebas
bash docs/evidencias/api/probar-api-sah.sh    'CONTRASEÑA_ADMIN'   # 46 pruebas (43 al repetir)
bash docs/evidencias/api/probar-api-csv.sh    'CONTRASEÑA_ADMIN'   # 8 pruebas (carga CSV)
```

Los scripts crean datos de prueba la primera vez y los reutilizan después
(la API no borra registros). El estudiante de prueba
`estudiante.api@sah-pruebas.com` queda **inactivo** al terminar, por lo que no
puede iniciar sesión.

Alternativa gráfica: importar `SAH-API.postman_collection.json` (55 peticiones
en 6 carpetas: Cursos, Empresas, Usuarios y permisos, Evaluaciones, Carga masiva CSV,
Asignaciones y avance) en Postman o
en la extensión **Thunder Client** de VS Code. El nombre de cada petición indica
el código HTTP esperado.

## Pruebas automáticas (JUnit)

`cd backend/spring-boot` y luego `.\mvnw.cmd clean test`: **37 pruebas, 0 fallos** (2026-11-27).
Incluye pruebas de integración con MockMvc contra MariaDB y GreenMail (ver la Sesión 8).

## Resultado de la última ejecución (2026-11-26)

- `probar-api-cursos.sh`: 21/21 correctas.
- `probar-api-sah.sh`: 46/46 correctas (primera ejecución) y 43/43 (repetición).
