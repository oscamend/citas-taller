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

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        List<Usuario> lista = usuarioRepository.findAll();
        model.addAttribute("usuarios", lista);
        return "lista-usuarios";
    }

    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String role,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (usuarioRepository.findByUsername(username).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "El usuario '" + username + "' ya existe.");
                return "redirect:/usuarios";
            }
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setUsername(username);
            nuevoUsuario.setPassword(passwordEncoder.encode(password));
            nuevoUsuario.setRole(role);
            usuarioRepository.save(nuevoUsuario);
            redirectAttributes.addFlashAttribute("exito", "Usuario creado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar el usuario.");
        }
        return "redirect:/usuarios";
    }

    // ... (El método /usuarios/actualizar debe redirigir a /usuarios)

    @PostMapping("/usuarios/cambiar-mi-password")
    public String cambiarMiPassword(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam("passwordActual") String passwordActual,
                                    @RequestParam("nuevaPassword") String nuevaPassword,
                                    RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                // REDIRIGIR A CITAS, NO A USUARIOS
                redirectAttributes.addFlashAttribute("error", "La contraseña actual es incorrecta.");
                return "redirect:/citas";
            }

            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuarioRepository.save(usuario);

            // REDIRIGIR A CITAS
            redirectAttributes.addFlashAttribute("exito", "¡Contraseña actualizada con éxito!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al intentar cambiar la contraseña.");
        }
        return "redirect:/citas";
    }
}
