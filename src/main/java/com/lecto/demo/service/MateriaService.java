package com.lecto.demo.service;

import com.lecto.demo.dto.MateriaRequestDto;
import com.lecto.demo.dto.MateriaResponseDto;
import com.lecto.demo.entity.Materia;
import com.lecto.demo.repository.MateriaRepository;
import com.lecto.demo.repository.NotaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MateriaService {

    private final MateriaRepository materiaRepository;
    private final NotaRepository notaRepository;

    // Inyección de dependencias por constructor
    public MateriaService(MateriaRepository materiaRepository, NotaRepository notaRepository) {
        this.materiaRepository = materiaRepository;
        this.notaRepository = notaRepository;
    }

    // 1. Listar todas las materias activas del usuario con su conteo de notas
    public List<MateriaResponseDto> listar(String usuarioId) {
        return materiaRepository.listarConConteo(usuarioId);
    }

    // 2. Obtener el detalle de una materia (lo que se devuelve al cliente)
    public MateriaResponseDto obtener(UUID id, String usuarioId) {
        return aDto(obtenerEntidad(id, usuarioId));
    }

    // 3. Crear una nueva materia
    @Transactional
    public MateriaResponseDto crear(MateriaRequestDto dto, String usuarioId) {
        // Verificar si ya existe para evitar duplicados (Conflict 409)
        if (materiaRepository.existsById(dto.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La materia ya existe");
        }

        // Construir la entidad copiando los datos del DTO
        Materia materia = new Materia();
        materia.setId(dto.getId());
        materia.setNombre(dto.getNombre());
        materia.setColor(dto.getColor());
        materia.setIcono(dto.getIcono());
        materia.setFechaCreacion(dto.getFechaCreacion());
        materia.setFechaModificacion(dto.getFechaModificacion());

        // Asignar el usuario del token y marcar como no eliminada
        materia.setUsuarioId(usuarioId);
        materia.setEliminado(false);

        return aDto(materiaRepository.save(materia));
    }

    // 4. Actualizar una materia existente
    @Transactional
    public MateriaResponseDto actualizar(UUID id, MateriaRequestDto dto, String usuarioId) {
        // Si no existe o no es del usuario, obtenerEntidad ya lanza 404
        Materia materia = obtenerEntidad(id, usuarioId);


        if (dto.getFechaModificacion().isBefore(materia.getFechaModificacion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La versión enviada es más antigua que la guardada");
        }

        // Solo se actualizan los campos editables:
        // fechaCreacion y usuarioId no se tocan nunca
        materia.setNombre(dto.getNombre());
        materia.setColor(dto.getColor());
        materia.setIcono(dto.getIcono());
        materia.setFechaModificacion(dto.getFechaModificacion());

        return aDto(materiaRepository.save(materia));
    }

    // 5. Eliminar (lógicamente) una materia
    @Transactional
    public void eliminar(UUID id, String usuarioId) {
        Materia materia = obtenerEntidad(id, usuarioId);

        // Regla del modelo: no se puede borrar una materia que todavía tiene notas
        if (notaRepository.existsByMateriaIdAndEliminadoFalse(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar una materia que tiene notas");
        }

        // Borrado la fila se conserva y se marca.

        materia.setEliminado(true);
        materia.setFechaModificacion(Instant.now());

        materiaRepository.save(materia);
    }



    // Busca la entidad filtrando también por usuario: si la materia es de otro,
    // el resultado viene vacío y se responde 404 (no se confirma que exista).
    private Materia obtenerEntidad(UUID id, String usuarioId) {
        return materiaRepository.findByIdAndUsuarioIdAndEliminadoFalse(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Materia no encontrada"));
    }

    // Convierte la entidad en el DTO de salida.
    // Así el JSON nunca expone usuario_id y siempre incluye cantidad_notas.
    private MateriaResponseDto aDto(Materia m) {
        long cantidadNotas = notaRepository.countByMateriaIdAndEliminadoFalse(m.getId());
        return new MateriaResponseDto(
                m.getId(), m.getNombre(), m.getColor(), m.getIcono(),
                m.getFechaCreacion(), m.getFechaModificacion(),
                m.isEliminado() ? 0 : 1, cantidadNotas);
    }
}