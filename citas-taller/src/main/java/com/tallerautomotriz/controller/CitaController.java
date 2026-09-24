package com.tallerautomotriz.controller;

import com.tallerautomotriz.model.Cita;
import com.tallerautomotriz.model.Cliente;
import com.tallerautomotriz.repository.CitaRepository;
import com.tallerautomotriz.repository.ClienteRepository;
import com.tallerautomotriz.repository.MecanicoRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class CitaController {

    private final CitaRepository repository;
    private final ClienteRepository clienteRepository;
    private final MecanicoRepository mecanicoRealRepository;

    public CitaController(CitaRepository repository,
                          ClienteRepository clienteRepository,
                          MecanicoRepository mecanicoRealRepository) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.mecanicoRealRepository = mecanicoRealRepository;
    }

    // =========================================================================
    // ACCIÓN: Listar clientes con Buscador por DNI y Teléfono
    // =========================================================================
    @GetMapping("/clientes")
    public String listarClientes(
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String telefono,
            Model model) {

        String filtroDni = (dni != null && !dni.trim().isEmpty()) ? dni.trim() : null;
        String filtroTelefono = (telefono != null && !telefono.trim().isEmpty()) ? telefono.trim() : null;

        List<Cliente> clientesFiltrados = clienteRepository.filtrarClientes(filtroDni, filtroTelefono);

        model.addAttribute("clientes", clientesFiltrados);
        model.addAttribute("paramDni", dni);
        model.addAttribute("paramTelefono", telefono);
        model.addAttribute("cliente", new Cliente());

        return "lista-clientes";
    }

    // =========================================================================
    // VISTA PRINCIPAL: Historial y Buscador de Citas (CON FILTRO Y ORDENACIÓN)
    // =========================================================================
    @GetMapping("/citas")
    public String listarCitas(
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) Long mecanicoId,
            @RequestParam(required = false) String ordenarFecha,
            Model model) {

        String filtroDni = (dni != null && !dni.trim().isEmpty()) ? dni.trim() : null;
        String filtroMatricula = (matricula != null && !matricula.trim().isEmpty()) ? matricula.trim() : null;

        // 1. Obtener citas filtradas inicialmente por DNI o Matrícula
        List<Cita> citasFiltradas = repository.filtrarCitas(filtroDni, filtroMatricula, null, null);

        // 2. FILTRO POR MECÁNICO (En memoria para no romper consultas personalizadas)
        if (mecanicoId != null) {
            citasFiltradas = citasFiltradas.stream()
                    .filter(c -> c.getMecanico() != null && c.getMecanico().getId().equals(mecanicoId))
                    .collect(Collectors.toList());
        }

        // 3. ORDENACIÓN CRONOLÓGICA (De menor a mayor / Próximas primero)
        if ("asc".equalsIgnoreCase(ordenarFecha)) {
            citasFiltradas.sort((c1, c2) -> {
                if (c1.getFechaHora() == null) return 1;
                if (c2.getFechaHora() == null) return -1;
                return c1.getFechaHora().compareTo(c2.getFechaHora());
            });
        }

        // Enviar todas las variables necesarias a la vista Thymeleaf
        model.addAttribute("citas", citasFiltradas);
        model.addAttribute("mecanicos", mecanicoRealRepository.findAll());
        model.addAttribute("paramDni", dni);
        model.addAttribute("paramMatricula", matricula);
        model.addAttribute("paramMecanicoId", mecanicoId);
        model.addAttribute("paramOrdenarFecha", ordenarFecha);

        return "lista-citas";
    }

    // =========================================================================
    // PASO 1 (GET): Inicializar variables para evitar Error 500 en Thymeleaf
    // =========================================================================
    @GetMapping("/citas/nuevo-cliente")
    public String mostrarPasoCliente(Model model) {
        model.addAttribute("dni", "");
        model.addAttribute("mostrarRegistro", false);
        return "cita-paso1-cliente";
    }

    // =========================================================================
    // PASO 1 (POST): Procesar DNI, alternar subpasos dinámicos
    // =========================================================================
    @PostMapping("/citas/verificar-cliente")
    public String verificarCliente(
            @RequestParam String dni,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String telefono,
            Model model) {

        if (dni == null || dni.trim().isEmpty()) {
            model.addAttribute("dni", "");
            model.addAttribute("mostrarRegistro", false);
            return "cita-paso1-cliente";
        }

        var clienteOpt = clienteRepository.findByDni(dni.trim());

        if (clienteOpt.isPresent()) {
            return "redirect:/citas/agendar?clienteId=" + clienteOpt.get().getId();
        }

        if (nombre != null && !nombre.trim().isEmpty() && apellido != null && !apellido.trim().isEmpty()) {
            Cliente nuevoCliente = new Cliente();
            nuevoCliente.setDni(dni.trim());
            nuevoCliente.setNombre(nombre.trim());
            nuevoCliente.setApellido(apellido.trim());
            nuevoCliente.setTelefono(telefono != null ? telefono.trim() : "");

            Cliente clienteGuardado = clienteRepository.save(nuevoCliente);
            return "redirect:/citas/agendar?clienteId=" + clienteGuardado.getId();
        } else {
            model.addAttribute("dni", dni.trim());
            model.addAttribute("mostrarRegistro", true);
            return "cita-paso1-cliente";
        }
    }

    // =========================================================================
    // PASO 2: Formulario para los datos mecánicos del vehículo y la cita
    // =========================================================================
    @GetMapping("/citas/agendar")
    public String mostrarPasoCita(@RequestParam Long clienteId, Model model) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        model.addAttribute("cliente", cliente);
        model.addAttribute("cita", new Cita());
        model.addAttribute("mecanicos", mecanicoRealRepository.findAll());

        return "cita-paso2-datos";
    }

    // =========================================================================
    // ACCIÓN: Guardar la Cita Final en la Base de Datos
    // =========================================================================
    @PostMapping("/citas/guardar")
    public String guardarCita(
            @RequestParam Long clienteId,
            @RequestParam String matricula,
            @RequestParam String marca,
            @RequestParam String tipoService,
            @RequestParam Long mecanicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora,
            @RequestParam String descripcionAveria,
            Model model,
            RedirectAttributes redirectAttributes) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente Inválido"));

        var mecanico = mecanicoRealRepository.findById(mecanicoId)
                .orElseThrow(() -> new IllegalArgumentException("Mecánico Inválido"));

        LocalDateTime inicioMenos30Min = fechaHora.minusMinutes(30);
        LocalDateTime finMas30Min = fechaHora.plusMinutes(30);

        Cita citaTemporal = new Cita();
        citaTemporal.setMatricula(matricula);
        citaTemporal.setMarca(marca);
        citaTemporal.setTipoService(tipoService);
        citaTemporal.setMecanico(mecanico);
        citaTemporal.setFechaHora(fechaHora);
        citaTemporal.setDescripcionAveria(descripcionAveria);

        List<Cita> mecSolapados = repository.findCitasSolapadas(mecanico, inicioMenos30Min, finMas30Min);
        if (!mecSolapados.isEmpty()) {
            model.addAttribute("error", "¡Conflicto de Horario! El mecánico " +
                    mecanico.getNombre() + " " + mecanico.getApellido() +
                    " ya está ocupado con una cita en ese rango de 30 minutos.");

            model.addAttribute("cliente", cliente);
            model.addAttribute("cita", citaTemporal);
            model.addAttribute("mecanicos", mecanicoRealRepository.findAll());
            return "cita-paso2-datos";
        }

        List<Cita> matSolapadas = repository.findMatriculaSolapada(matricula.trim(), inicioMenos30Min, finMas30Min);
        if (!matSolapadas.isEmpty()) {
            model.addAttribute("error", "¡Conflicto de Vehículo! El coche con matrícula " +
                    matricula + " ya cuenta con otra cita programada en esa misma franja horaria.");

            model.addAttribute("cliente", cliente);
            model.addAttribute("cita", citaTemporal);
            model.addAttribute("mecanicos", mecanicoRealRepository.findAll());
            return "cita-paso2-datos";
        }

        Cita nuevaCita = new Cita();
        nuevaCita.setCliente(cliente);
        nuevaCita.setMatricula(matricula);
        nuevaCita.setMarca(marca);
        nuevaCita.setTipoService(tipoService);
        nuevaCita.setMecanico(mecanico);
        nuevaCita.setFechaHora(fechaHora);
        nuevaCita.setDescripcionAveria(descripcionAveria);

        repository.save(nuevaCita);
        redirectAttributes.addFlashAttribute("exito", "Cita agendada correctamente.");

        return "redirect:/citas";
    }

    // =========================================================================
    // ACCIÓN: Eliminar Cita por ID
    // =========================================================================
    @PostMapping("/citas/eliminar/{id}")
    public String eliminarCita(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
        return "redirect:/citas";
    }

    // =========================================================================
    // ACCIÓN: Eliminar Cliente por ID
    // =========================================================================
    @PostMapping("/clientes/eliminar/{id}")
public String eliminarCliente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
        if (clienteRepository.existsById(id)) {
            clienteRepository.deleteById(id);
            clienteRepository.flush();
        }
    } catch (DataIntegrityViolationException e) {
        redirectAttributes.addFlashAttribute(
                "errorEliminacion",
                "No se puede eliminar este cliente porque tiene citas asociadas."
        );
    }

    return "redirect:/clientes";
}

    // =========================================================================
    // ACCIÓN (GET): Mostrar el formulario de edición con los datos del cliente
    // =========================================================================
    @GetMapping("/clientes/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id));

        model.addAttribute("cliente", cliente);
        return "editar-cliente";
    }

    // =========================================================================
    // ACCIÓN (POST): Procesar y guardar los datos modificados del cliente
    // =========================================================================
    @PostMapping("/clientes/actualizar")
    public String actualizarCliente(@ModelAttribute Cliente clienteForm, Model model) {

        Optional<Cliente> clienteExistenteConDni = clienteRepository.findByDni(clienteForm.getDni().trim());
        if (clienteExistenteConDni.isPresent() && !clienteExistenteConDni.get().getId().equals(clienteForm.getId())) {
            model.addAttribute("errorClienteDuplicado", "El DNI/NIE introducido ya pertenece a otro cliente registrado.");
            model.addAttribute("cliente", clienteForm);
            return "editar-cliente";
        }

        if (clienteForm.getTelefono() != null && !clienteForm.getTelefono().trim().isEmpty()) {
            Optional<Cliente> clienteExistenteConTelefono = clienteRepository.findByTelefono(clienteForm.getTelefono().trim());

            if (clienteExistenteConTelefono.isPresent() && !clienteExistenteConTelefono.get().getId().equals(clienteForm.getId())) {
                model.addAttribute("errorClienteDuplicado", "El número de teléfono introducido ya está asociado a otro cliente.");
                model.addAttribute("cliente", clienteForm);
                return "editar-cliente";
            }
        }

        clienteRepository.save(clienteForm);
        return "redirect:/clientes";
    }

    // =========================================================================
    // ACCIÓN (GET): Mostrar el formulario de edición con los datos de la cita
    // =========================================================================
    @GetMapping("/citas/editar/{id}")
    public String mostrarFormularioEditarCita(@PathVariable Long id, Model model) {
        Cita cita = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada: " + id));

        model.addAttribute("cita", cita);
        model.addAttribute("cliente", cita.getCliente());
        model.addAttribute("mecanicos", mecanicoRealRepository.findAll());

        return "editar-cita";
    }

    // =========================================================================
    // ACCIÓN (POST): Procesar y actualizar los cambios de la cita
    // =========================================================================
    @PostMapping("/citas/actualizar")
    public String actualizarCita(
            @RequestParam Long id,
            @RequestParam Long clienteId,
            @RequestParam String matricula,
            @RequestParam String marca,
            @RequestParam String tipoService,
            @RequestParam Long mecanicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora,
            @RequestParam String descripcionAveria,
            Model model,
            RedirectAttributes redirectAttributes) {

        Cita citaExistente = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cita Inválida"));

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente Inválido"));

        var mecanico = mecanicoRealRepository.findById(mecanicoId)
                .orElseThrow(() -> new IllegalArgumentException("Mecánico Inválido"));

        LocalDateTime inicioMenos30Min = fechaHora.minusMinutes(30);
        LocalDateTime finMas30Min = fechaHora.plusMinutes(30);

        Cita citaEditadaConErrores = new Cita();
        citaEditadaConErrores.setId(id);
        citaEditadaConErrores.setMatricula(matricula);
        citaEditadaConErrores.setMarca(marca);
        citaEditadaConErrores.setTipoService(tipoService);
        citaEditadaConErrores.setMecanico(mecanico);
        citaEditadaConErrores.setFechaHora(fechaHora);
        citaEditadaConErrores.setDescripcionAveria(descripcionAveria);

        List<Cita> mecSolapados = repository.findCitasSolapadas(mecanico, inicioMenos30Min, finMas30Min);
        for (Cita c : mecSolapados) {
            if (!c.getId().equals(id)) {
                model.addAttribute("error", "No se pudo actualizar: El mecánico elegido ya tiene asignado un servicio en ese horario.");
                model.addAttribute("cita", citaEditadaConErrores);
                model.addAttribute("cliente", cliente);
                model.addAttribute("mecanicos", mecanicoRealRepository.findAll());
                return "editar-cita";
            }
        }

        List<Cita> matSolapadas = repository.findMatriculaSolapada(matricula.trim(), inicioMenos30Min, finMas30Min);
        for (Cita c : matSolapadas) {
            if (!c.getId().equals(id)) {
                model.addAttribute("error", "No se pudo actualizar: El vehículo ya tiene otra cita en esa franja de 30 minutos.");
                model.addAttribute("cita", citaEditadaConErrores);
                model.addAttribute("cliente", cliente);
                model.addAttribute("mecanicos", mecanicoRealRepository.findAll());
                return "editar-cita";
            }
        }

        citaExistente.setCliente(cliente);
        citaExistente.setMatricula(matricula);
        citaExistente.setMarca(marca);
        citaExistente.setTipoService(tipoService);
        citaExistente.setMecanico(mecanico);
        citaExistente.setFechaHora(fechaHora);
        citaExistente.setDescripcionAveria(descripcionAveria);

        repository.save(citaExistente);
        redirectAttributes.addFlashAttribute("exito", "Cita actualizada correctamente.");

        return "redirect:/citas";
    }

    // =========================================================================
    // ACCIÓN (POST): Guardar un nuevo cliente desde el modal
    // =========================================================================
    @PostMapping("/clientes/guardar")
    public String guardarNuevoClienteFromModal(@ModelAttribute("cliente") Cliente nuevoCliente, RedirectAttributes redirectAttributes) {

        Optional<Cliente> clienteExistenteConDni = clienteRepository.findByDni(nuevoCliente.getDni().trim());
        if (clienteExistenteConDni.isPresent()) {
            redirectAttributes.addFlashAttribute("errorModalDuplicado", "No se pudo crear: El DNI/NIE ya pertenece a un cliente registrado.");
            return "redirect:/clientes";
        }

        if (nuevoCliente.getTelefono() != null && !nuevoCliente.getTelefono().trim().isEmpty()) {
            Optional<Cliente> clienteExistenteConTelefono = clienteRepository.findByTelefono(nuevoCliente.getTelefono().trim());
            if (clienteExistenteConTelefono.isPresent()) {
                redirectAttributes.addFlashAttribute("errorModalDuplicado", "No se pudo crear: El número de teléfono ya está asociado a otro cliente.");
                return "redirect:/clientes";
            }
        }

        clienteRepository.save(nuevoCliente);
        return "redirect:/clientes";
    }

    // =========================================================================
    // ENDPOINT AJAX: Validar DNI y Teléfono en tiempo real
    // =========================================================================
    @GetMapping("/api/clientes/validar-duplicados")
    @ResponseBody
    public java.util.Map<String, Object> validarDuplicados(
            @RequestParam String dni,
            @RequestParam String telefono) {

        java.util.Map<String, Object> respuesta = new java.util.HashMap<>();
        respuesta.put("dniDuplicado", false);
        respuesta.put("telefonoDuplicado", false);

        if (dni != null && !dni.trim().isEmpty()) {
            if (clienteRepository.findByDni(dni.trim()).isPresent()) {
                respuesta.put("dniDuplicado", true);
            }
        }

        if (telefono != null && !telefono.trim().isEmpty()) {
            if (clienteRepository.findByTelefono(telefono.trim()).isPresent()) {
                respuesta.put("telefonoDuplicado", true);
            }
        }

        return respuesta;
    }
}