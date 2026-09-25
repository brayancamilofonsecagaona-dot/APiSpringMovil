package com.lecto.demo.repository;

import com.lecto.demo.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    // Se usa para generar un nombre de usuario que no choque con otro existente
    boolean existsByNombreUsuario(String nombreUsuario);
}