package com.lecto.demo.dto;

// Este DTO sirve para devolver la información del perfil al cliente
// (incluyendo el ID, ya que es el perfil propio del usuario)

import java.time.Instant;

public class UsuarioResponseDto {

    private String id;
    private String correo;
    private String nombre;
    private String nombreUsuario;
    private String fotoUrl;
    private Instant fechaCreacion;

    // Constructor vacío
    public UsuarioResponseDto() {}

    // Constructor completo
    public UsuarioResponseDto(String id, String correo, String nombre, String nombreUsuario,
                              String fotoUrl, Instant fechaCreacion) {
        this.id = id;
        this.correo = correo;
        this.nombre = nombre;
        this.nombreUsuario = nombreUsuario;
        this.fotoUrl = fotoUrl;
        this.fechaCreacion = fechaCreacion;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}