-- ============================================================
-- SECURITY AWARENESS HUB (SAH)
-- MIGRACION 001: roles, empresas por usuario y asignaciones
-- ============================================================
-- Alinea la base de datos con las entidades JPA de Spring Boot
-- (backend/spring-boot/src/main/java/com/sah/entity), que usan
-- spring.jpa.hibernate.ddl-auto=validate.
--
-- * NO borra la base de datos ni tablas existentes.
-- * Se puede ejecutar mas de una vez (CREATE ... IF NOT EXISTS,
--   ADD COLUMN IF NOT EXISTS, inserciones condicionadas).
-- * Requiere MariaDB 10.4 o superior (XAMPP).
--
-- Ejecucion (PowerShell):
--   Get-Content database\migracion_001_roles_empresas_asignaciones.sql -Raw |
--     C:\xampp\mysql\bin\mysql.exe -u root
-- ============================================================

CREATE DATABASE IF NOT EXISTS security_awareness_hub
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE security_awareness_hub;


-- ============================================================
-- 1. EMPRESAS (debe existir antes de la relacion con usuarios)
-- ============================================================

CREATE TABLE IF NOT EXISTS empresas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    nit VARCHAR(30) NOT NULL UNIQUE,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30) DEFAULT NULL,
    direccion VARCHAR(200) DEFAULT NULL,
    tipo ENUM('privada', 'publica', 'mixta') NOT NULL DEFAULT 'privada',
    estado ENUM('activo', 'inactivo') NOT NULL DEFAULT 'activo',
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 2. USUARIOS
-- ============================================================

CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol ENUM('admin', 'admin_empresa', 'empleado') NOT NULL DEFAULT 'empleado',
    estado ENUM('activo', 'inactivo') NOT NULL DEFAULT 'activo',
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

-- 2.1 Roles: el esquema antiguo usaba ('usuario', 'admin').
--     Primero se amplia el ENUM, luego se convierte 'usuario' -> 'empleado'
--     (Estudiante) y por ultimo se dejan solo los tres roles del sistema.
ALTER TABLE usuarios
    MODIFY rol ENUM('usuario', 'admin', 'admin_empresa', 'empleado')
        NOT NULL DEFAULT 'empleado';

UPDATE usuarios SET rol = 'empleado' WHERE rol = 'usuario';

ALTER TABLE usuarios
    MODIFY rol ENUM('admin', 'admin_empresa', 'empleado')
        NOT NULL DEFAULT 'empleado';

-- 2.2 Empresa a la que pertenece el usuario (NULL para el admin del sistema).
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS empresa_id INT NULL AFTER apellidos;

CREATE INDEX IF NOT EXISTS idx_usuarios_empresa ON usuarios(empresa_id);

-- La FK se crea solo si no existe (MariaDB no soporta ADD CONSTRAINT IF NOT EXISTS
-- para claves foraneas de forma fiable, por eso se consulta information_schema).
SET @existe_fk := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'security_awareness_hub'
      AND TABLE_NAME = 'usuarios'
      AND CONSTRAINT_NAME = 'fk_usuarios_empresa'
);
SET @sql := IF(@existe_fk = 0,
    'ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresas(id)
        ON UPDATE CASCADE ON DELETE RESTRICT',
    'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.3 Administrador inicial (solo si no hay ningun admin).
--     Contrasena: la misma del script original (hash BCrypt).
INSERT INTO usuarios (nombres, apellidos, correo, password, rol, estado)
SELECT 'Administrador', 'SAH', 'admin@sah.com',
       '$2y$12$awlzns0sJO9ICU3Y.SfiXe4Ia3PhyHVLMsuOgw4rmfMv3ivoR/lcS',
       'admin', 'activo'
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE rol = 'admin');


-- ============================================================
-- 3. CURSOS
-- ============================================================

CREATE TABLE IF NOT EXISTS cursos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT NOT NULL,
    nivel_dificultad ENUM('basico', 'intermedio', 'avanzado') NOT NULL DEFAULT 'basico',
    duracion INT NOT NULL,
    numero_modulos INT NOT NULL DEFAULT 1,
    porcentaje_aprobacion INT NOT NULL DEFAULT 70,
    estado ENUM('activo', 'inactivo') NOT NULL DEFAULT 'activo',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

INSERT INTO cursos (titulo, descripcion, nivel_dificultad, duracion,
                    numero_modulos, porcentaje_aprobacion, estado)
SELECT 'Fundamentos de Ciberseguridad',
       'Introducción a los conceptos básicos de seguridad digital.',
       'basico', 5, 5, 70, 'activo'
WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE titulo = 'Fundamentos de Ciberseguridad');

INSERT INTO cursos (titulo, descripcion, nivel_dificultad, duracion,
                    numero_modulos, porcentaje_aprobacion, estado)
SELECT 'Seguridad de Contraseñas',
       'Aprende a crear, gestionar y proteger contraseñas seguras para evitar accesos no autorizados.',
       'basico', 4, 4, 70, 'activo'
WHERE NOT EXISTS (SELECT 1 FROM cursos WHERE titulo = 'Seguridad de Contraseñas');


-- ============================================================
-- 4. EVALUACIONES, PREGUNTAS Y OPCIONES
-- ============================================================

CREATE TABLE IF NOT EXISTS evaluaciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    curso_id INT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT NOT NULL,
    porcentaje_aprobacion INT NOT NULL DEFAULT 70,
    estado ENUM('activo', 'inactivo') NOT NULL DEFAULT 'activo',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evaluaciones_curso
        FOREIGN KEY (curso_id) REFERENCES cursos(id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS preguntas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    evaluacion_id INT NOT NULL,
    enunciado TEXT NOT NULL,
    orden INT NOT NULL DEFAULT 1,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_preguntas_evaluacion
        FOREIGN KEY (evaluacion_id) REFERENCES evaluaciones(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS opciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pregunta_id INT NOT NULL,
    texto VARCHAR(255) NOT NULL,
    es_correcta TINYINT(1) NOT NULL DEFAULT 0,
    orden INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_opciones_pregunta
        FOREIGN KEY (pregunta_id) REFERENCES preguntas(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_evaluaciones_curso ON evaluaciones(curso_id);
CREATE INDEX IF NOT EXISTS idx_preguntas_evaluacion ON preguntas(evaluacion_id);
CREATE INDEX IF NOT EXISTS idx_opciones_pregunta ON opciones(pregunta_id);


-- ============================================================
-- 5. ASIGNACIONES (cursos o evaluaciones asignados a estudiantes)
-- ============================================================
-- Reglas que tambien se garantizan en la base de datos:
-- * Una asignacion de tipo 'curso' tiene curso_id y no evaluacion_id,
--   y una de tipo 'evaluacion' lo contrario (CHECK).
-- * Un estudiante no puede tener el mismo curso o la misma evaluacion
--   asignados dos veces (UNIQUE; los NULL no cuentan como duplicados).
-- * No se puede borrar un usuario, curso o evaluacion que tenga
--   asignaciones (ON DELETE RESTRICT): se desactivan en su lugar.

CREATE TABLE IF NOT EXISTS asignaciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    curso_id INT NULL,
    evaluacion_id INT NULL,
    tipo ENUM('curso', 'evaluacion') NOT NULL,
    fecha_asignacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('pendiente', 'progreso', 'completado') NOT NULL DEFAULT 'pendiente',

    CONSTRAINT fk_asignaciones_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_asignaciones_curso
        FOREIGN KEY (curso_id) REFERENCES cursos(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_asignaciones_evaluacion
        FOREIGN KEY (evaluacion_id) REFERENCES evaluaciones(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT uq_asignaciones_usuario_curso UNIQUE (usuario_id, curso_id),
    CONSTRAINT uq_asignaciones_usuario_evaluacion UNIQUE (usuario_id, evaluacion_id),

    CONSTRAINT chk_asignaciones_tipo CHECK (
        (tipo = 'curso' AND curso_id IS NOT NULL AND evaluacion_id IS NULL)
        OR
        (tipo = 'evaluacion' AND evaluacion_id IS NOT NULL AND curso_id IS NULL)
    )
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- VERIFICACION
-- ============================================================

SHOW TABLES;
SELECT id, correo, rol, estado, empresa_id FROM usuarios;
