package com.lecto.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class NotaRequestDto {

    @NotNull(message = "El id es obligatorio")
    private UUID id;

    @NotNull(message = "El materiaId es obligatorio")
    private UUID materiaId;

    @NotBlank(message = "El título no puede estar en blanco")
    @Size(max = 120, message = "El título no puede superar los 120 caracteres")
    private String titulo;

    @NotNull(message = "El texto no puede ser nulo")
    @Size(max = 50000, message = "El texto es demasiado largo")
    private String texto; // Puede estar vacío (""), pero no null por restricciones de base de datos

    @Size(max = 500, message = "La URL de la imagen es demasiado larga")
    private String imagenUrl; // Opcional, admite null

    @NotNull(message = "La fecha de creación es obligatoria")
    private Instant fechaCreacion;

    @NotNull(message = "La fecha de modificación es obligatoria")
    private Instant fechaModificacion;

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
}