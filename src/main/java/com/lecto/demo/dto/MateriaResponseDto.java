package com.lecto.demo.dto;

import java.time.Instant;
import java.util.UUID;

public class MateriaResponseDto {
    private UUID id;
    private String nombre;
    private String color;
    private String icono;
    private Instant fechaCreacion;
    private Instant fechaModificacion;
    private boolean eliminado;
    private long cantidadNotas;

    // Constructor con todos los parámetros para mapear fácilmente las consultas
    public MateriaResponseDto(UUID id, String nombre, String color, String icono,
                              Instant fechaCreacion, Instant fechaModificacion,
                              boolean eliminado, long cantidadNotas) {
        this.id = id;
        this.nombre = nombre;
        this.color = color;
        this.icono = icono;
        this.fechaCreacion = fechaCreacion;
        this.fechaModificacion = fechaModificacion;
        this.eliminado = eliminado;
        this.cantidadNotas = cantidadNotas;
    }
    public MateriaResponseDto(){

    }
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

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public long getCantidadNotas() {
        return cantidadNotas;
    }

    public void setCantidadNotas(long cantidadNotas) {
        this.cantidadNotas = cantidadNotas;
    }
}
