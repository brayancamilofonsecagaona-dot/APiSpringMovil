package com.lecto.demo.service;

import com.lecto.demo.dto.NotaRequestDto;
import com.lecto.demo.dto.NotaResponseDto;
import com.lecto.demo.entity.Nota;
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
public class NotaService {

    private final NotaRepository notaRepository;
    private final MateriaRepository materiaRepository;

    public NotaService(NotaRepository notaRepository, MateriaRepository materiaRepository) {
        this.notaRepository = notaRepository;
        this.materiaRepository = materiaRepository;
    }

    // 1. Listar notas con filtros dinámicos (por materia y/o palabra clave)
    public List<NotaResponseDto> listar(String uid, UUID materiaId, String q) {
        List<Nota> notas;

        // Evaluamos qué parámetros nos llegaron para llamar al método adecuado del repositorio
        if (q != null && !q.isBlank()) {
            // La búsqueda por palabra clave va sobre el texto de la nota (tarea 1.4)
            notas = notaRepository.findByUsuarioIdAndEliminadoFalseAndTextoContainingIgnoreCase(uid, q);
            // Si además mandaron materia, se filtra el resultado en memoria
            if (materiaId != null) {
                notas = notas.stream()
                        .filter(n -> materiaId.equals(n.getMateriaId()))
                        .toList();
            }
        } else if (materiaId != null) {
            notas = notaRepository.findByUsuarioIdAndMateriaIdAndEliminadoFalseOrderByFechaCreacionDesc(uid, materiaId);
        } else {
            notas = notaRepository.findByUsuarioIdAndEliminadoFalseOrderByFechaCreacionDesc(uid);
        }

        return notas.stream().map(this::mapearADto).toList();
    }

    // 2. Obtener una nota específica (404 si no existe, no es suya o está eliminada)
    public NotaResponseDto obtener(UUID id, String uid) {
        return mapearADto(buscarNotaValidada(id, uid));
    }

    // 3. Crear nota validando la materia
    @Transactional
    public NotaResponseDto crear(NotaRequestDto dto, String uid) {
        // Evita que se creen duplicados si la app reintenta el envío
        if (notaRepository.existsById(dto.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La nota ya existe");
        }

        // Validamos que la materia exista y sea del usuario
        validarMateria(dto.getMateriaId(), uid);

        Nota nota = new Nota();
        nota.setId(dto.getId());
        nota.setUsuarioId(uid);
        nota.setMateriaId(dto.getMateriaId());
        nota.setTitulo(dto.getTitulo());

        // Si el texto viene nulo (por seguridad extra), lo guardamos vacío
        nota.setTexto(dto.getTexto() != null ? dto.getTexto() : "");
        nota.setImagenUrl(dto.getImagenUrl());
        nota.setFechaCreacion(dto.getFechaCreacion());
        nota.setFechaModificacion(dto.getFechaModificacion());
        nota.setEliminado(false);

        return mapearADto(notaRepository.save(nota));
    }

    // 4. Actualizar con manejo de conflictos de versión y validación de materia
    @Transactional
    public NotaResponseDto actualizar(UUID id, NotaRequestDto dto, String uid) {
        Nota nota = buscarNotaValidada(id, uid);

        // Regla del modelo: gana la fecha_modificacion más reciente.
        // Si lo que llega es más viejo que lo guardado, se rechaza.
        if (dto.getFechaModificacion().isBefore(nota.getFechaModificacion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La versión enviada es más antigua que la guardada");
        }

        // Si el usuario decidió cambiar la nota de materia, validamos la nueva materia
        if (!nota.getMateriaId().equals(dto.getMateriaId())) {
            validarMateria(dto.getMateriaId(), uid);
            nota.setMateriaId(dto.getMateriaId());
        }

        nota.setTitulo(dto.getTitulo());
        nota.setTexto(dto.getTexto() != null ? dto.getTexto() : "");
        nota.setImagenUrl(dto.getImagenUrl());

        // Se guarda la fecha del cliente, la misma con la que se resuelven los conflictos.
        // fechaCreacion y usuarioId no se tocan nunca.
        nota.setFechaModificacion(dto.getFechaModificacion());

        return mapearADto(notaRepository.save(nota));
    }

    // 5. Eliminar (Borrado lógico)
    @Transactional
    public void eliminar(UUID id, String uid) {
        Nota nota = buscarNotaValidada(id, uid);

        // La fila se conserva y se marca; la fecha se actualiza para que
        // la sincronización detecte el borrado.
        nota.setEliminado(true);
        nota.setFechaModificacion(Instant.now());
        notaRepository.save(nota);
    }

    // Métodos Privados Auxiliares

    // Busca la nota filtrando también por usuario: si es de otro, responde 404
    private Nota buscarNotaValidada(UUID id, String uid) {
        return notaRepository.findByIdAndUsuarioIdAndEliminadoFalse(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota no encontrada"));
    }

    // Impide crear o mover notas a una materia que no existe o es de otro usuario
    private void validarMateria(UUID materiaId, String uid) {
        if (!materiaRepository.existsByIdAndUsuarioIdAndEliminadoFalse(materiaId, uid)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "La materia indicada no existe o no te pertenece");
        }
    }

    private NotaResponseDto mapearADto(Nota nota) {
        return new NotaResponseDto(
                nota.getId(),
                nota.getMateriaId(),
                nota.getTitulo(),
                nota.getTexto(),
                nota.getImagenUrl(),
                nota.getFechaCreacion(),
                nota.getFechaModificacion(),
                nota.isEliminado()
        );
    }
}