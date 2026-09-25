package com.lecto.demo.service;

import com.lecto.demo.dto.MateriaResponseDto;
import com.lecto.demo.dto.NotaResponseDto;
import com.lecto.demo.dto.SyncResponseDto;
import com.lecto.demo.entity.Materia;
import com.lecto.demo.entity.Nota;
import com.lecto.demo.repository.MateriaRepository;
import com.lecto.demo.repository.NotaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SyncService {

    private final MateriaRepository materiaRepository;
    private final NotaRepository notaRepository;

    public SyncService(MateriaRepository materiaRepository, NotaRepository notaRepository) {
        this.materiaRepository = materiaRepository;
        this.notaRepository = notaRepository;
    }

    // Devuelve los cambios del usuario desde la fecha indicada.
    // Si no mandan fecha, es la primera sincronización: se devuelve todo lo activo.
    public SyncResponseDto sincronizar(String uid, Instant desde) {

        // La hora se toma ANTES de consultar, para no perder cambios
        // que ocurran mientras se arma la respuesta
        Instant ahora = Instant.now();

        List<Materia> materias;
        List<Nota> notas;

        if (desde == null) {
            // Primera vez: no tiene sentido mandar cosas borradas que la app nunca conoció
            materias = materiaRepository.findByUsuarioIdAndEliminadoFalseOrderByNombre(uid);
            notas = notaRepository.findByUsuarioIdAndEliminadoFalseOrderByFechaCreacionDesc(uid);
        } else {
            // Sincronizaciones siguientes: se incluye lo eliminado
            materias = materiaRepository.findByUsuarioIdAndFechaModificacionGreaterThan(uid, desde);
            notas = notaRepository.findByUsuarioIdAndFechaModificacionGreaterThan(uid, desde);
        }

        return new SyncResponseDto(
                materias.stream().map(this::mapearMateria).toList(),
                notas.stream().map(this::mapearNota).toList(),
                ahora
        );
    }

    // Métodos auxiliares

    // El conteo de notas va en 0: en la sincronización la app lo calcula de su base local
    private MateriaResponseDto mapearMateria(Materia m) {
        return new MateriaResponseDto(
                m.getId(), m.getNombre(), m.getColor(), m.getIcono(),
                m.getFechaCreacion(), m.getFechaModificacion(), m.isEliminado(), 0);
    }

    private NotaResponseDto mapearNota(Nota n) {
        return new NotaResponseDto(
                n.getId(), n.getMateriaId(), n.getTitulo(), n.getTexto(), n.getImagenUrl(),
                n.getFechaCreacion(), n.getFechaModificacion(), n.isEliminado());
    }
}