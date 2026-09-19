package com.lecto.demo.repository;
import com.lecto.demo.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

//Estas son las interfaces que son como los contartos
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

}