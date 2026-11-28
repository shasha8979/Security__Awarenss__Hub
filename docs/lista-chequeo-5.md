# Lista de Chequeo 5 aplicada a SAH

Fuente: `Lista_de_Chequeo_5_v2.xlsx` (SENA, Tecnólogo en Análisis y Desarrollo de Software 228118,
Fase 3 – Ejecución). Valoración del producto, 15 minutos. Cada ítem se marca CUMPLE SÍ / NO con observaciones.

**Última revisión:** 2026-11-27 — rama `Andres` — `.\mvnw.cmd clean test`: 37 pruebas, 0 fallos.

Leyenda: ✅ cumple con evidencia · 🟡 parcial · ❌ falta

## Parte 1: indicadores técnicos del producto

| # | Indicador (resumido) | Estado | Evidencia | Qué falta |
|---|---|---|---|---|
| 1 | Interfaz: usabilidad, accesibilidad, consistencia visual, diseño responsivo | 🟡 | Interfaz Vaadin (`views/`) con diseño común (`MainLayout`), etiquetas en todos los campos, menú por rol y textos en español (H11) | Probar la interfaz en celular y revisar el contraste. El `frontend/` HTML del PHP no consume la API |
| 2 | Seguridad, rendimiento, mantenibilidad, disponibilidad | ✅ | Spring Security + BCrypt; credenciales en `.env` y usuario `sah_app` con permisos mínimos; 500 sin detalles internos; log en `INFO`; carga CSV en bloques con límites de tamaño (`sesion-8-correo-csv.md` §2) | Medir el tiempo de una carga de 1000 filas (R4) |
| 3 | Arquitectura en capas (Controller, Service, Repository, Entity) | ✅ | Paquetes `controller`, `service`, `repository`, `model`, `dto`, `exception`. Las vistas Vaadin también usan servicios (H4 corregido) | Organización por capa y no por módulo: justificada en D2 |
| 4 | Reglas de negocio en servicios, inyección de dependencias, POO | ✅ | Reglas solo en `XxxService`; inyección por constructor con `private final`; DTOs `record`; excepciones propias | — |
| 5 | Persistencia con Spring Data JPA, entidades, repositorios y relaciones | ✅ | `@Entity` + `JpaRepository` con MariaDB; `@ManyToOne` (usuario–empresa, asignación–usuario/curso/evaluación) con `@EntityGraph` (`sesion-6-persistencia.md`) | Entidades `Pregunta` y `Opcion` (tablas ya creadas) |
| 6 | Autenticación, autorización y protección de recursos | ✅ | HTTP Basic para `/api/**`, formulario para Vaadin; `@RolesAllowed` por vista; pruebas 401/403 en scripts, Postman y `CargaCsvIntegracionTest` / `AsignacionIntegracionTest` | — |
| 7 | Calidad: listas de verificación, revisiones técnicas, estándares, control de versiones | 🟡 | Git con ramas (`main`, `Andres`); `.gitignore` (incluye `.env`); nombres en español; lista de verificación en `docs/calidad/hallazgos-y-riesgos.md` §3 | Hacer commit de los cambios del 2026-11-27 y abrir un PR revisado hacia `main` |
| 8 | Pruebas funcionales, de integración y de aceptación documentadas, con hallazgos | ✅ | 37 pruebas JUnit (unitarias, MockMvc con MariaDB, GreenMail); colección Postman (55 peticiones); scripts con resultado esperado/obtenido; hallazgos H1–H12; script `probar-api-csv.sh` 8/8 y 413 probado; pruebas de aceptación en el navegador A1–A4 (`sesion-8-correo-csv.md` §6) | Probar el envío con un SMTP real (Gmail) |
| 9 | Mejora a partir de la verificación: acciones correctivas/preventivas, riesgos | ✅ | `docs/calidad/hallazgos-y-riesgos.md`: hallazgo → acción → verificación, y matriz de riesgos R1–R8 | Mantenerlo actualizado en cada entrega |

### Ítems que las guías no cubren paso a paso

- **1 (interfaz):** resuelto con Vaadin (decisión D1).
- **5 (relaciones JPA):** `@ManyToOne` + `@EntityGraph` (D9).
- **7 y 9:** documentos de calidad en `docs/calidad/`.
- **8:** pruebas automáticas JUnit/MockMvc (D14), además de Postman.

## Parte 2: trabajo en equipo Scrum y presentación

No se resuelve con código. En el instrumento estos ítems se numeran de nuevo desde el 7.

| # | Indicador | Preparación sugerida |
|---|---|---|
| 7 | Cumple el tiempo de la presentación | Ensayar con cronómetro: 15 minutos |
| 8 | Participan todos los integrantes | Repartir la demostración: login y roles, CSV y correo, avance del estudiante, reportes, pruebas |
| 9 | Apropiación del proyecto | Cada integrante explica una decisión de `migracion-decisiones.md` |
| 10 | Código de Ética del Ingeniero de Software | Mencionar la protección de datos (BCrypt, permisos mínimos, sin claves en Git) |
| 11 | Recibe aportes y argumenta desacuerdos | Llevar la matriz de riesgos como base de la discusión |
| 12 | Recursos audiovisuales | Diapositivas con diagrama de capas y capturas de las pruebas |
| 13 | Manejo del espacio y contacto visual | — |
| 14 | Tono, ritmo y volumen de voz | — |

## Cómo usar esta lista

1. Al cerrar cada sesión, actualizar la columna "Evidencia".
2. Antes de la entrega final, recorrer los 9 ítems técnicos y marcar SÍ / NO como lo hará el evaluador.
