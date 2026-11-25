
package com.sah.service;

import com.sah.entity.Usuario;
import com.sah.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.Locale;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    private static final Set<String> ROLES_PERMITIDOS = Set.of(
            "admin",
            "admin_empresa",
            "empleado"
    );

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "No existe un usuario con ese correo."
                        )
                );

        if (!"activo".equalsIgnoreCase(usuario.getEstado())) {
            throw new UsernameNotFoundException(
                    "La cuenta se encuentra inactiva."
            );
        }

        if (usuario.getRol() == null
                || !ROLES_PERMITIDOS.contains(
                        usuario.getRol().toLowerCase(Locale.ROOT)
                )) {

            throw new UsernameNotFoundException(
                    "El rol del usuario no es válido."
            );
        }

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPassword())
                .roles(usuario.getRol().toUpperCase(Locale.ROOT))
                .build();
    }
}
