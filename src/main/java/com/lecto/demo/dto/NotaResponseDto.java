package com.lecto.demo.dto;

import java.time.Instant;
import java.util.UUID;

public class NotaResponseDto {

    private UUID id;
    private UUID materiaId;
    private String titulo;
    private String texto;
    private String imagenUrl;
    private Instant fechaCreacion;
    private Instant fechaModificacion;
    private boolean eliminado;

    // Constructor vacío
    public NotaResponseDto() {}

    // Constructor completo
    public NotaResponseDto(UUID id, UUID materiaId, String titulo, String texto, String imagenUrl,
                           Instant fechaCreacion, Instant fechaModificacion, boolean eliminado) {
        this.id = id;
        this.materiaId = materiaId;
        this.titulo = titulo;
        this.texto = texto;
        this.imagenUrl = imagenUrl;
        this.fechaCreacion = fechaCreacion;
        this.fechaModificacion = fechaModificacion;
        this.eliminado = eliminado;
    }

    // Getters y Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMateriaId() { return materiaId; }
    public void setMateriaId(UUID materiaId) { this.materiaId = materiaId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Instant getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(Instant fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public boolean isEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }
}