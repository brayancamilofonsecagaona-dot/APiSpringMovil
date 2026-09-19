package com.lecto.demo.repository;

import com.lecto.demo.entity.Nota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotaRepository extends JpaRepository<Nota, UUID> {

    // Se usa antes de borrar una materia: si tiene notas activas, la API responde 409
    boolean existsByMateriaIdAndEliminadoFalse(UUID materiaId);

    // Cuenta las notas activas de una materia, para el campo cantidad_notas del detalle
    long countByMateriaIdAndEliminadoFalse(UUID materiaId);
}