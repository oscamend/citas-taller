package com.tallerautomotriz.repository;

import com.tallerautomotriz.model.Cita;
import com.tallerautomotriz.model.Mecanico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    // CORREGIDO: Se unificó el nombre del parámetro `:inicioMenos30Min` en una sola línea limpia
    @Query("SELECT c FROM Cita c WHERE c.mecanico = :mecanico " +
            "AND c.fechaHora < :fin " +
            "AND c.fechaHora > :inicioMenos30Min")
    List<Cita> findCitasSolapadas(
            @Param("mecanico") Mecanico mecanico,
            @Param("inicioMenos30Min") LocalDateTime inicioMenos30Min,
            @Param("fin") LocalDateTime fin
    );

    // Filtro multiparámetro dinámico a través de la relación Cliente
    @Query("SELECT c FROM Cita c JOIN c.cliente cl WHERE " +
            "(:dni IS NULL OR cl.dni LIKE %:dni%) AND " +
            "(:matricula IS NULL OR c.matricula LIKE %:matricula%) AND " +
            "(:tipoServicio IS NULL OR c.tipoService = :tipoServicio) AND " +
            "(:mecanicoId IS NULL OR c.mecanico.id = :mecanicoId)")
    List<Cita> filtrarCitas(
            @Param("dni") String dni,
            @Param("matricula") String matricula,
            @Param("tipoServicio") String tipoServicio,
            @Param("mecanicoId") Long mecanicoId
    );

    // CORREGIDO: Aseguramos también la consistencia del parámetro aquí
    @Query("SELECT c FROM Cita c WHERE c.matricula = :matricula " +
            "AND c.fechaHora < :fin " +
            "AND c.fechaHora > :inicioMenos30Min")
    List<Cita> findMatriculaSolapada(
            @Param("matricula") String matricula,
            @Param("inicioMenos30Min") LocalDateTime inicioMenos30Min,
            @Param("fin") LocalDateTime fin
    );
}