package com.tallerautomotriz.controller;

import com.tallerautomotriz.model.Mecanico;
import com.tallerautomotriz.repository.MecanicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/mecanicos")
public class MecanicoController {

    @Autowired
    private MecanicoRepository mecanicoRepository;

    /**
     * Lista todos los mecánicos en el sistema y prepara el formulario para añadir.
     */
    @GetMapping
    public String listarMecanicos(Model model) {
        List<Mecanico> lista = mecanicoRepository.findAll();
        model.addAttribute("mecanicos", lista);

        // Si no viene ya un mecánico con errores desde los métodos POST, inicializamos uno vacío
        if (!model.containsAttribute("mecanico")) {
            model.addAttribute("mecanico", new Mecanico());
        }

        // Nos aseguramos de que si no existe la variable de apertura automática, esté en falso por defecto
        if (!model.containsAttribute("abrirModalError")) {
            model.addAttribute("abrirModalError", false);
        }

        return "lista-mecanicos";
    }

    /**
     * Guarda un nuevo mecánico en la base de datos (CON VALIDACIÓN DE CAMPOS).
     */
    @PostMapping("/guardar")
    public String guardarMecanico(@ModelAttribute Mecanico mecanico, Model model, RedirectAttributes redirectAttributes) {
        // Validamos que nombre y apellido no estén vacíos en el servidor
        if (mecanico.getNombre() == null || mecanico.getNombre().trim().isEmpty() ||
                mecanico.getApellido() == null || mecanico.getApellido().trim().isEmpty()) {

            model.addAttribute("error", "No se pudo guardar: El Nombre y el Apellido son campos obligatorios.");
            model.addAttribute("mecanicos", mecanicoRepository.findAll());
            model.addAttribute("mecanico", mecanico); // Mantenemos los datos escritos para no vaciar las cajas

            // Forzamos a que JavaScript sepa que tiene que volver a abrir el modal obligatoriamente
            model.addAttribute("abrirModalError", true);
            return "lista-mecanicos";
        }

        try {
            mecanicoRepository.save(mecanico);
            redirectAttributes.addFlashAttribute("exito", "Mecánico registrado correctamente en el sistema.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar el mecánico debido a un error de base de datos.");
        }
        return "redirect:/mecanicos";
    }

    /**
     * Elimina un mecánico por su ID.
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarMecanico(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            if (mecanicoRepository.existsById(id)) {
                mecanicoRepository.deleteById(id);
                redirectAttributes.addFlashAttribute("exito", "Mecánico eliminado correctamente.");
            } else {
                redirectAttributes.addFlashAttribute("error", "El mecánico que intentas eliminar no existe.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar. El mecánico podría tener citas o reparaciones asignadas.");
        }
        return "redirect:/mecanicos";
    }

    /**
     * Actualiza un mecánico existente (CON VALIDACIÓN DE CAMPOS).
     */
    @PostMapping("/actualizar")
    public String actualizarMecanico(@ModelAttribute Mecanico mecanico, Model model, RedirectAttributes redirectAttributes) {
        // Validamos también al actualizar
        if (mecanico.getNombre() == null || mecanico.getNombre().trim().isEmpty() ||
                mecanico.getApellido() == null || mecanico.getApellido().trim().isEmpty()) {

            model.addAttribute("error", "No se pudo actualizar: El Nombre y el Apellido no pueden quedar vacíos.");
            model.addAttribute("mecanicos", mecanicoRepository.findAll());
            model.addAttribute("mecanico", new Mecanico()); // Reseteamos el del modal para evitar conflictos

            // Al actualizar erróneamente mostramos el error arriba de la tabla sin abrir el modal de inserción
            model.addAttribute("abrirModalError", false);
            return "lista-mecanicos";
        }

        try {
            if (mecanico.getId() != null && mecanicoRepository.existsById(mecanico.getId())) {
                mecanicoRepository.save(mecanico);
                redirectAttributes.addFlashAttribute("exito", "Mecánico actualizado correctamente.");
            } else {
                redirectAttributes.addFlashAttribute("error", "El mecánico que intentas actualizar no existe.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo actualizar el mecánico debido a un error interno.");
        }
        return "redirect:/mecanicos";
    }
}