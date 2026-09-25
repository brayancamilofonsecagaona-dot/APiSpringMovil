package com.lecto.demo.repository;

import com.lecto.demo.entity.Nota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotaRepository extends JpaRepository<Nota, UUID> {

    // Se usa antes de borrar una materia: si tiene notas activas, la API responde 409
    boolean existsByMateriaIdAndEliminadoFalse(UUID materiaId);

    // Cuenta las notas activas de una materia, para el campo cantidad_notas del detalle
    long countByMateriaIdAndEliminadoFalse(UUID materiaId);

    // Se usa al eliminar la cuenta: borra todas las notas del usuario
    void deleteByUsuarioId(String usuarioId);

    // Para el detalle, editar y borrar, verificando que la nota sea del usuario
    Optional<Nota> findByIdAndUsuarioIdAndEliminadoFalse(UUID id, String usuarioId);

    // Listado de todas las notas del usuario
    List<Nota> findByUsuarioIdAndEliminadoFalseOrderByFechaCreacionDesc(String usuarioId);

    // Listado filtrado por materia
    List<Nota> findByUsuarioIdAndMateriaIdAndEliminadoFalseOrderByFechaCreacionDesc(String usuarioId, UUID materiaId);

    // Búsqueda por palabra clave dentro del texto de las notas
    List<Nota> findByUsuarioIdAndEliminadoFalseAndTextoContainingIgnoreCase(String usuarioId, String texto);

    // Para la sincronización: trae lo que cambió desde una fecha,
    // incluidas las notas eliminadas (la app necesita saber qué se borró)
    List<Nota> findByUsuarioIdAndFechaModificacionGreaterThan(String usuarioId, Instant desde);



}