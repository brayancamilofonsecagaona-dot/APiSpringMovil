package com.lecto.demo.repository;

import com.lecto.demo.dto.MateriaResponseDto;
import com.lecto.demo.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Interfaz que maneja las operaciones de base de datos para la entidad Materia
public interface MateriaRepository extends JpaRepository<Materia, UUID> {

    // Consulta personalizada usando JPQL para listar materias y contar sus notas activas de un usuario
    @Query("""
        SELECT new com.lecto.demo.dto.MateriaResponseDto(
            m.id, m.nombre, m.color, m.icono,
            m.fechaCreacion, m.fechaModificacion, m.eliminado,
            (SELECT COUNT(n) FROM Nota n WHERE n.materiaId = m.id AND n.eliminado = false)
        )
        FROM Materia m
        WHERE m.usuarioId = :usuarioId AND m.eliminado = false
        ORDER BY m.nombre
    """)
    List<MateriaResponseDto> listarConConteo(@Param("usuarioId") String usuarioId);

    // Método derivado: Spring arma el SQL a partir del nombre, sin escribir la consulta.
    // Sirve para consultar, editar o borrar verificando que la materia sea del usuario.
    Optional<Materia> findByIdAndUsuarioIdAndEliminadoFalse(UUID id, String usuarioId);
}

//JPQL Es un lenguaje de consultas parecido a SQL, pero que trabaja sobre las clases Java, no sobre las tablas
// Se escriben nombres de clases y campos Java; Hibernate lo traduce a SQL.
// El "SELECT new ..." construye el DTO directamente con su constructor completo.