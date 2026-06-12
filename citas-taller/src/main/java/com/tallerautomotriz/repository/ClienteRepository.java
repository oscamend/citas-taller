package com.tallerautomotriz.repository;

import com.tallerautomotriz.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Conserva tu método de buscar DNI exacto para el flujo de citas
    Optional<Cliente> findByDni(String dni);
    Optional<Cliente> findByTelefono(String telefono);
    /**
     * NUEVO: Filtro dinámico para el buscador de la tabla de clientes
     */
    @Query("SELECT c FROM Cliente c WHERE " +
            "(:dni IS NULL OR c.dni LIKE %:dni%) AND " +
            "(:telefono IS NULL OR c.telefono LIKE %:telefono%)")
    List<Cliente> filtrarClientes(
            @Param("dni") String dni,
            @Param("telefono") String telefono
    );
}