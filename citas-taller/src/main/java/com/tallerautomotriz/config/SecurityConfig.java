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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByUsername(username)
                .map(u -> {
                    String limpiarRol = u.getRole().replace("ROLE_", "");
                    return User.builder()
                            .username(u.getUsername())
                            .password(u.getPassword())
                            .roles(limpiarRol)
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. Rutas estáticas y login públicas
                        .requestMatchers("/login", "/css/**", "/js/**").permitAll()

                        // 2. RUTA DE CAMBIO DE CONTRASEÑA: Permitir a todos los autenticados (ADMIN y USER)
                        .requestMatchers("/usuarios/cambiar-mi-password").hasAnyRole("ADMIN", "USER")

                        // 3. Gestión de mecánicos y usuarios: SOLO ADMINISTRADOR
                        // (Nota: el orden importa, esta línea va después de la específica de arriba)
                        .requestMatchers("/mecanicos/**", "/usuarios/**").hasRole("ADMIN")

                        // 4. Modificaciones críticas de citas: SOLO ADMINISTRADOR
                        .requestMatchers("/citas/editar/**", "/citas/eliminar/**").hasRole("ADMIN")

                        // 5. Creación y lectura de citas: TANTO USER COMO ADMIN
                        .requestMatchers("/citas", "/citas/nuevo-cliente", "/citas/guardar").hasAnyRole("ADMIN", "USER")

                        // 6. Cualquier otra ruta requiere estar autenticado
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
