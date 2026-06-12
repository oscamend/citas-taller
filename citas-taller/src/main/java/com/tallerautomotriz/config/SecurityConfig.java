package com.tallerautomotriz.config;

import com.tallerautomotriz.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UsuarioRepository usuarioRepository;

    public SecurityConfig(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // 1. El encriptador de contraseñas para la Base de Datos
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. El buscador de usuarios en la tabla de MySQL
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByUsername(username)
                .map(u -> {
                    // Quitamos el prefijo "ROLE_" si existe en la BD para que no se duplique
                    String limpiarRol = u.getRole().replace("ROLE_", "");

                    return User.builder()
                            .username(u.getUsername())
                            .password(u.getPassword())
                            .roles(limpiarRol) // 🔑 Cambiado de .authorities() a .roles()
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    // 3. El filtro de seguridad principal
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. Rutas estáticas y login públicas
                        .requestMatchers("/login", "/css/**", "/js/**").permitAll()

                        // 2. Gestión de mecánicos y usuarios: SOLO ADMINISTRADOR
                        .requestMatchers("/mecanicos/**", "/usuarios/**").hasRole("ADMIN")

                        // 3. Modificaciones críticas de citas (Editar / Eliminar): SOLO ADMINISTRADOR
                        .requestMatchers("/citas/editar/**", "/citas/eliminar/**").hasRole("ADMIN")

                        // 4. Creación y lectura de citas (Asistente y Guardar): TANTO USER COMO ADMIN
                        .requestMatchers("/citas", "/citas/nuevo-cliente", "/citas/guardar").hasAnyRole("ADMIN", "USER")

                        // 5. Cualquier otra ruta requiere estar autenticado
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/citas", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}