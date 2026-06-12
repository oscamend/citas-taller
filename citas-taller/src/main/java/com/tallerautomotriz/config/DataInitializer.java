package com.tallerautomotriz.config;

import com.tallerautomotriz.model.Usuario;
import com.tallerautomotriz.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Genera el usuario administrador maestro inicial si la tabla está limpia
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(System.getenv("ADMIN_PASSWORD")));
            admin.setRole("ROLE_ADMIN");

            usuarioRepository.save(admin);
            System.out.println("============== ¡ADMINISTRADOR INICIAL CREADO CON ÉXITO! ==============");
        }
    }
}