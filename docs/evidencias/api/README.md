# API REST — Security Awareness Hub

Evidencia de las guías 4 (API REST y códigos HTTP), 5 (capas) y 7 (validación,
manejo de errores y seguridad).

## Endpoints

Base: `http://localhost:8080/api` — autenticación **HTTP Basic** (usuario = correo).

| Recurso | GET lista | GET {id} | POST | PUT {id} | PATCH {id}/estado | DELETE |
|---|---|---|---|---|---|---|
| `/cursos` | autenticado | autenticado | admin | admin | admin | 405 |
| `/evaluaciones` (`?cursoId=`) | autenticado | autenticado | admin | admin | admin | 405 |
| `/empresas` | admin | admin | admin | admin | admin | 405 |
| `/usuarios` (`?rol=`) | admin | admin | admin | admin | admin | 405 |

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
```

Los scripts crean datos de prueba la primera vez y los reutilizan después
(la API no borra registros). El estudiante de prueba
`estudiante.api@sah-pruebas.com` queda **inactivo** al terminar, por lo que no
puede iniciar sesión.

Alternativa gráfica: importar `SAH-API.postman_collection.json` (42 peticiones
en 4 carpetas: Cursos, Empresas, Usuarios y permisos, Evaluaciones) en Postman o
en la extensión **Thunder Client** de VS Code. El nombre de cada petición indica
el código HTTP esperado.

## Resultado de la última ejecución (2026-11-26)

- `probar-api-cursos.sh`: 21/21 correctas.
- `probar-api-sah.sh`: 46/46 correctas (primera ejecución) y 43/43 (repetición).
