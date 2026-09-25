package com.lecto.demo.dto;

import java.time.Instant;
import java.util.List;

// Lo que devuelve GET /sync: todo lo que cambió desde la última sincronización.
// Incluye los registros eliminados, para que la app los borre de su base local.
public class SyncResponseDto {

    private List<MateriaResponseDto> materias;
    private List<NotaResponseDto> notas;

    // Hora del servidor: la app la guarda y la manda como ?desde= la próxima vez
    private Instant sincronizadoEn;

    public SyncResponseDto() {}

    public SyncResponseDto(List<MateriaResponseDto> materias, List<NotaResponseDto> notas, Instant sincronizadoEn) {
        this.materias = materias;
        this.notas = notas;
        this.sincronizadoEn = sincronizadoEn;
    }

    // Getters y Setters
    public List<MateriaResponseDto> getMaterias() { return materias; }
    public void setMaterias(List<MateriaResponseDto> materias) { this.materias = materias; }

    public List<NotaResponseDto> getNotas() { return notas; }
    public void setNotas(List<NotaResponseDto> notas) { this.notas = notas; }

    public Instant getSincronizadoEn() { return sincronizadoEn; }
    public void setSincronizadoEn(Instant sincronizadoEn) { this.sincronizadoEn = sincronizadoEn; }
}