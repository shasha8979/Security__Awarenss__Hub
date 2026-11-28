<?php
/**
 * Restablece la contraseña de un usuario de SAH (herramienta de desarrollo).
 *
 * Uso (PowerShell, con MySQL de XAMPP encendido):
 *   C:\xampp\php\php.exe database\restablecer_clave.php admin@sah.com
 *
 * Pide la nueva contraseña por consola, la cifra con BCrypt (compatible con
 * Spring Security) y la guarda. La contraseña no queda escrita en ningún archivo.
 *
 * Solo se ejecuta desde la consola: htdocs es una carpeta pública y este
 * script no debe poder abrirse desde el navegador.
 */

if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}

$correo = $argv[1] ?? '';
if ($correo === '') {
    fwrite(STDERR, "Uso: php database/restablecer_clave.php correo@dominio.com\n");
    exit(1);
}

echo "Nueva contraseña para $correo (mínimo 8 caracteres): ";
$clave = trim((string) fgets(STDIN));

if (strlen($clave) < 8) {
    fwrite(STDERR, "La contraseña debe tener al menos 8 caracteres. No se cambió nada.\n");
    exit(1);
}

try {
    $bd = new PDO(
        'mysql:host=localhost;dbname=security_awareness_hub;charset=utf8mb4',
        'root',
        '',
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
    );

    $sentencia = $bd->prepare('UPDATE usuarios SET password = ? WHERE correo = ?');
    $sentencia->execute([password_hash($clave, PASSWORD_BCRYPT), $correo]);

    if ($sentencia->rowCount() === 0) {
        fwrite(STDERR, "No existe un usuario con el correo $correo.\n");
        exit(1);
    }

    $estado = $bd->prepare('SELECT rol, estado FROM usuarios WHERE correo = ?');
    $estado->execute([$correo]);
    $usuario = $estado->fetch(PDO::FETCH_ASSOC);

    echo "Listo: contraseña actualizada (rol {$usuario['rol']}, estado {$usuario['estado']}).\n";
    if ($usuario['estado'] !== 'activo') {
        echo "Atención: la cuenta está INACTIVA y no podrá iniciar sesión.\n";
    }
} catch (PDOException $e) {
    fwrite(STDERR, "No se pudo conectar o actualizar: " . $e->getMessage() . "\n");
    exit(1);
}
