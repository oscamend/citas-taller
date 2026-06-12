package com.tallerautomotriz.repository;

import com.tallerautomotriz.model.Mecanico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MecanicoRepository extends JpaRepository<Mecanico, Long> {
    // Aquí heredamos todos los métodos como .findAll(), .findById(), etc.
}