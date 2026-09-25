package com.lecto.demo.dto;
//Este DTO se usa para recibir los datos en las peticiones PUT de actualización. Incluye validaciones básicas
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Solo lo que el usuario puede editar de su perfil.
// El correo y el id vienen de Firebase, no se cambian desde aquí.
public class UsuarioRequestDto {

    // Los máximos coinciden con las columnas de la tabla usuarios
    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    private String nombre;

    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    @Size(min = 3, max = 40, message = "El nombre de usuario debe tener entre 3 y 40 caracteres")
    private String nombreUsuario;

    // Admite null: la foto es opcional
    @Size(max = 500, message = "La URL de la foto es demasiado larga")
    private String fotoUrl;

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
}