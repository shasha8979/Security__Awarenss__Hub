-- ============================================================
-- Usuario propio de la aplicacion (Sesion 7: seguridad)
-- ============================================================
-- La aplicacion Spring Boot NO debe conectarse como root.
-- Este usuario solo puede leer y modificar datos de
-- security_awareness_hub: no puede crear, alterar ni borrar
-- tablas, ni tocar otras bases de datos.
--
-- Uso (desde la consola de XAMPP o phpMyAdmin, como root):
--   1. Cambia 'CAMBIA_ESTA_CLAVE' por una clave segura.
--   2. Ejecuta el script.
--   3. Copia backend/spring-boot/.env.example a .env y escribe
--      la misma clave en DB_PASSWORD. El archivo .env no se sube
--      a Git (.gitignore).
--
-- Se puede ejecutar varias veces sin error.
-- ============================================================

CREATE USER IF NOT EXISTS 'sah_app'@'localhost'
    IDENTIFIED BY 'CAMBIA_ESTA_CLAVE';

GRANT SELECT, INSERT, UPDATE, DELETE
    ON security_awareness_hub.*
    TO 'sah_app'@'localhost';

FLUSH PRIVILEGES;
