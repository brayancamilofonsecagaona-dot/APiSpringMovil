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
    // Contrato del JSON acordado con el equipo: 1 = activo, 0 = eliminado
    private int estado;

    // Constructor vacío
    public NotaResponseDto() {}

    // Constructor completo
    public NotaResponseDto(UUID id, UUID materiaId, String titulo, String texto, String imagenUrl,
                           Instant fechaCreacion, Instant fechaModificacion, int estado) {
        this.id = id;
        this.materiaId = materiaId;
        this.titulo = titulo;
        this.texto = texto;
        this.imagenUrl = imagenUrl;
        this.fechaCreacion = fechaCreacion;
        this.fechaModificacion = fechaModificacion;
        this.estado = estado;
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

    public int getEstado() { return estado; }
    public void setEstado(int estado) { this.estado = estado; }
}