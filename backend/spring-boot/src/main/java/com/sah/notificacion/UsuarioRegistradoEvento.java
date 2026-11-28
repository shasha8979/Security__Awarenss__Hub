package com.sah.notificacion;

/**
 * Se publica cuando la carga masiva crea un usuario.
 * La contraseña temporal solo viaja en memoria hasta el correo;
 * en la base de datos se guarda cifrada con BCrypt.
 */
public record UsuarioRegistradoEvento(
        String correo,
        String nombres,
        String claveTemporal
) {

    /** Evita que la clave aparezca si el evento se escribe en un log. */
    @Override
    public String toString() {
        return "UsuarioRegistradoEvento[correo=" + correo + "]";
    }
}
