package com.emplanorte.service;

import com.emplanorte.model.Usuario;
import com.emplanorte.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<Usuario> login(String correo, String contrasena) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            // Compara la contraseña en texto plano con el hash guardado (BCrypt)
            if (usuario.getActivo() && passwordEncoder.matches(contrasena, usuario.getContrasenaHash())) {
                return Optional.of(usuario);
            }
        }
        return Optional.empty();
    }

    /**
     * Registro de un nuevo usuario. El formato del correo y las reglas de
     * complejidad de la contraseña se validan en la capa HTTP (AuthController)
     * antes de llegar aquí. Este método garantiza que el correo no exista,
     * cifra la contraseña con BCrypt (nunca se guarda en texto plano) y persiste.
     */
    public Usuario registrar(String correo, String contrasena, String nombre, String rol) {
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw new RuntimeException("El correo ya está registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(passwordEncoder.encode(contrasena));
        usuario.setRol(rol != null && !rol.isBlank() ? rol : "administrador");
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }
}
