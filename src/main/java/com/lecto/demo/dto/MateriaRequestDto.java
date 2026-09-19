package com.lecto.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
// La libreria Jakarta Validation  sirve basicamente para ayudar a Spring a validar datos automaticamente
import java.time.Instant;
import java.util.UUID;

// Un dto un objeto simple que su  único propósito es transportar datos de entrada o salida en los endpoints (sin lógica de negocio).
public class MateriaRequestDto {

    @NotNull(message = "El ID no puede ser nulo")
    private UUID id;

    // @NotBlank ya cubre null, vacío y solo espacios: no hace falta @NotNull
    @NotBlank(message = "El nombre no puede estar en blanco")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    private String nombre;

    // Validamos que sea un color hexadecimal válido (ej. #FF5733)
    // @Pattern no valida null, por eso @NotBlank sigue siendo necesario aquí
    @NotBlank(message = "El color no puede estar en blanco")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "El color debe ser un hexadecimal válido (ej. #FF5733)")
    private String color;

    @NotBlank(message = "El icono no puede estar en blanco")
    @Size(max = 30, message = "El icono no puede superar los 30 caracteres")
    private String icono;

    @NotNull(message = "La fecha de creación es obligatoria")
    private Instant fechaCreacion;

    @NotNull(message = "La fecha de modificación es obligatoria")
    private Instant fechaModificacion;

    // Constructor vacío obligatorio
    public MateriaRequestDto() {
    }

    // --- Getters y Setters para que Spring pueda leer/escribir los datos ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcono() {
        return icono;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(Instant fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}