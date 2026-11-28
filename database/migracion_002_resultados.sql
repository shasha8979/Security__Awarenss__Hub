-- ============================================================
-- SECURITY AWARENESS HUB (SAH)
-- MIGRACION 002: resultados de evaluaciones
-- Modulo: Evaluaciones, resultados y reportes (Integrante 3)
-- ============================================================
-- * NO borra ni modifica tablas existentes: solo crea tablas nuevas.
-- * Se puede ejecutar mas de una vez (IF NOT EXISTS).
-- * Requiere que ya existan: usuarios, evaluaciones, preguntas, opciones.
--
-- Reglas acordadas por el equipo:
--  - Un empleado puede presentar la misma evaluacion varias veces;
--    cada intento se guarda con su numero (1, 2, 3...).
--  - Nota = respuestas_correctas / total_preguntas * 100.
--  - Aprueba si la nota es >= evaluaciones.porcentaje_aprobacion.
--    Ese porcentaje se copia en porcentaje_requerido al presentar,
--    para que el historial no cambie si el admin lo modifica despues.
--  - Para estadisticas y reportes cuenta el MEJOR intento.
--  - Se guarda la opcion elegida en cada pregunta (resultado_respuestas).
--    Se copia el texto de la pregunta y de la opcion porque el admin
--    puede editar o borrar preguntas despues; por eso las FK a
--    preguntas/opciones quedan en NULL (ON DELETE SET NULL).
--
-- Ejecucion (PowerShell):
--   Get-Content database\migracion_002_resultados.sql -Raw |
--     C:\xampp\mysql\bin\mysql.exe -u root
-- ============================================================

USE security_awareness_hub;


-- ============================================================
-- TABLA: RESULTADOS (un registro por intento)
-- ============================================================

CREATE TABLE IF NOT EXISTS resultados (
    id INT AUTO_INCREMENT PRIMARY KEY,

    usuario_id INT NOT NULL,

    evaluacion_id INT NOT NULL,

    intento INT NOT NULL DEFAULT 1,

    respuestas_correctas INT NOT NULL,

    total_preguntas INT NOT NULL,

    porcentaje DECIMAL(5,2) NOT NULL,

    porcentaje_requerido INT NOT NULL,

    aprobado TINYINT(1) NOT NULL DEFAULT 0,

    fecha_presentacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_resultados_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_resultados_evaluacion
        FOREIGN KEY (evaluacion_id)
        REFERENCES evaluaciones(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_resultados_usuario_evaluacion_intento
        UNIQUE (usuario_id, evaluacion_id, intento),

    CONSTRAINT chk_resultados_porcentaje
        CHECK (porcentaje BETWEEN 0 AND 100),

    CONSTRAINT chk_resultados_correctas
        CHECK (respuestas_correctas BETWEEN 0 AND total_preguntas)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_resultados_evaluacion ON resultados(evaluacion_id);


-- ============================================================
-- TABLA: RESULTADO_RESPUESTAS (opcion elegida en cada pregunta)
-- ============================================================

CREATE TABLE IF NOT EXISTS resultado_respuestas (
    id INT AUTO_INCREMENT PRIMARY KEY,

    resultado_id INT NOT NULL,

    pregunta_id INT NULL,

    opcion_id INT NULL,

    enunciado_pregunta TEXT NOT NULL,

    texto_opcion VARCHAR(255) NULL,

    es_correcta TINYINT(1) NOT NULL DEFAULT 0,

    CONSTRAINT fk_respuestas_resultado
        FOREIGN KEY (resultado_id)
        REFERENCES resultados(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_respuestas_pregunta
        FOREIGN KEY (pregunta_id)
        REFERENCES preguntas(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_respuestas_opcion
        FOREIGN KEY (opcion_id)
        REFERENCES opciones(id)
        ON DELETE SET NULL

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_respuestas_resultado ON resultado_respuestas(resultado_id);


-- ============================================================
-- VERIFICACION
-- ============================================================

SHOW TABLES;
