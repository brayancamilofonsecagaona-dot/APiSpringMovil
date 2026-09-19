package com.lecto.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "usuarios")
public class Usuario {

    // El UID de Firebase: lo genera el proveedor, no la base
    @Id
    @Column(columnDefinition = "TEXT")
    private String id;

    @Column(nullable = false, length = 120, unique = true)
    private String correo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 40, unique = true)
    private String nombreUsuario;

    // TEXT en PostgreSQL, sin límite de longitud; admite null
    @Column(columnDefinition = "TEXT")
    private String fotoUrl;

    @Column(nullable = false)
    private Instant fechaCreacion;

    // JPA necesita el constructor vacío para instanciar por reflexión
    public Usuario() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}