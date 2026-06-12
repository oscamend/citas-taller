package com.tallerautomotriz.controller;

import com.tallerautomotriz.model.Usuario;
import com.tallerautomotriz.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 1. Muestra la lista completa de usuarios en el sistema.
     */
    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        List<Usuario> lista = usuarioRepository.findAll();
        model.addAttribute("usuarios", lista);
        return "lista-usuarios";
    }

    /**
     * 2. Registra un nuevo usuario en el sistema con encriptación BCrypt.
     */
    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String role,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (usuarioRepository.findByUsername(username).isPresent()) {
                redirectAttributes.addFlashAttribute("errorUsuario", "El nombre de usuario '" + username + "' ya está registrado.");
                return "redirect:/usuarios";
            }

            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setUsername(username);
            nuevoUsuario.setPassword(passwordEncoder.encode(password));
            nuevoUsuario.setRole(role);

            usuarioRepository.save(nuevoUsuario);
            redirectAttributes.addFlashAttribute("exitoUsuario", "La cuenta de usuario se ha creado correctamente.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorUsuario", "Ocurrió un error inesperado al intentar guardar el usuario.");
        }
        return "redirect:/usuarios";
    }

    /**
     * NUEVO: 2B. Actualiza los datos de perfil (Nombre de usuario y Rol).
     * Solo permite la acción si el usuario autenticado es un ADMINISTRADOR.
     */
    @PostMapping("/usuarios/actualizar")
    public String actualizarUsuario(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam Long id,
                                    @RequestParam String username,
                                    @RequestParam String role,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Verificación estricta de seguridad en Backend: ¿Es Administrador?
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                redirectAttributes.addFlashAttribute("errorUsuario", "Operación rechazada: Solo los administradores pueden editar los datos de los usuarios.");
                return "redirect:/usuarios";
            }

            Usuario usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("El usuario a editar no existe."));

            // Si cambia el nombre de usuario, comprobar que el nuevo no esté duplicado en otra cuenta distinta
            if (!usuario.getUsername().equals(username) && usuarioRepository.findByUsername(username).isPresent()) {
                redirectAttributes.addFlashAttribute("errorUsuario", "El nombre de usuario '" + username + "' ya lo está usando otra cuenta.");
                return "redirect:/usuarios";
            }

            usuario.setUsername(username);
            usuario.setRole(role);
            usuarioRepository.save(usuario);

            redirectAttributes.addFlashAttribute("exitoUsuario", "Los datos del usuario se han actualizado correctamente.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorUsuario", "Error al intentar actualizar los datos del usuario.");
        }
        return "redirect:/usuarios";
    }

    /**
     * 3. Elimina una cuenta de acceso por su ID asignado.
     */
    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            if (usuarioRepository.existsById(id)) {
                usuarioRepository.deleteById(id);
                redirectAttributes.addFlashAttribute("exitoUsuario", "Usuario eliminado correctamente.");
            } else {
                redirectAttributes.addFlashAttribute("errorUsuario", "El usuario que intentas eliminar no existe.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorUsuario", "No se puede eliminar el usuario. Es posible que esté vinculado a otros registros del taller.");
        }
        return "redirect:/usuarios";
    }

    /**
     * 4. Cambiar Contraseña de Terceros / Administrador
     */
    @PostMapping("/usuarios/cambiar-password")
    public String resetearPassword(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam(value = "id", required = false) Long id,
                                   @RequestParam Map<String, String> allParams,
                                   RedirectAttributes redirectAttributes) {
        try {
            String passwordFinal = null;

            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (!key.equals("id") && !key.equals("_csrf") && value != null && !value.trim().isEmpty()) {
                    passwordFinal = value;
                    if (key.toLowerCase().contains("nue") || key.toLowerCase().contains("pass")) {
                        passwordFinal = value;
                    }
                }
            }

            if (passwordFinal == null || passwordFinal.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorUsuario", "La contraseña no puede estar vacía.");
                return "redirect:/usuarios";
            }

            Usuario usuario;
            if (id != null) {
                usuario = usuarioRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
            } else {
                usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                        .orElseThrow(() -> new IllegalArgumentException("El usuario autenticado no existe."));
            }

            usuario.setPassword(passwordEncoder.encode(passwordFinal));
            usuarioRepository.save(usuario);

            redirectAttributes.addFlashAttribute("exitoUsuario", "La contraseña se ha actualizado correctamente.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorUsuario", "No se pudo actualizar la contraseña.");
        }
        return "redirect:/usuarios";
    }

    /**
     * 5. Cambia la contraseña del propio usuario conectado.
     */
    @PostMapping("/usuarios/cambiar-mi-password")
    public String cambiarMiPassword(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam("passwordActual") String passwordActual,
                                    @RequestParam("nuevaPassword") String nuevaPassword,
                                    RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado."));

            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                redirectAttributes.addFlashAttribute("errorUsuario", "La contraseña actual introducida no es correcta.");
                return "redirect:/usuarios";
            }

            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuarioRepository.save(usuario);

            redirectAttributes.addFlashAttribute("exitoUsuario", "¡Tu contraseña se ha actualizado con éxito!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorUsuario", "Ocurrió un error al intentar cambiar tu contraseña.");
        }
        return "redirect:/usuarios";
    }
}