package com.lecto.demo.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.lecto.demo.dto.UsuarioRequestDto;
import com.lecto.demo.dto.UsuarioResponseDto;
import com.lecto.demo.entity.Usuario;
import com.lecto.demo.repository.MateriaRepository;
import com.lecto.demo.repository.NotaRepository;
import com.lecto.demo.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final MateriaRepository materiaRepository;
    private final NotaRepository notaRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          MateriaRepository materiaRepository,
                          NotaRepository notaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.materiaRepository = materiaRepository;
        this.notaRepository = notaRepository;
    }

    // Crea el registro del usuario la primera vez que entra con un token válido.
    // Se llama desde el filtro, así ningún endpoint falla por la llave foránea.
    @Transactional
    public void asegurarUsuario(FirebaseToken token) {
        String uid = token.getUid();

        // Si el usuario ya existe en la base de datos, no hacemos nada
        if (usuarioRepository.existsById(uid)) {
            return;
        }

        // Si no existe, lo creamos con los datos de Firebase
        Usuario usuario = new Usuario();
        usuario.setId(uid);

        // El correo no debería venir nulo con login de correo y contraseña,
        // pero la columna es NOT NULL, así que se arma uno de respaldo
        String correo = token.getEmail();
        if (correo == null || correo.isBlank()) {
            correo = uid + "@lecto.local";
        }
        usuario.setCorreo(correo);

        // Si el nombre viene nulo o vacío, le ponemos un valor por defecto
        String nombre = token.getName();
        if (nombre == null || nombre.isBlank()) {
            nombre = "Usuario Lecto";
        }
        usuario.setNombre(nombre);

        usuario.setNombreUsuario(generarNombreUsuario(correo));
        usuario.setFechaCreacion(Instant.now());

        usuarioRepository.save(usuario);
        log.info("Usuario creado automáticamente: {}", uid);
    }

    // 1. Obtener el perfil del usuario autenticado (solo lectura, sin transacción)
    public UsuarioResponseDto obtener(String uid) {
        return mapearADto(buscarEntidad(uid));
    }

    // 2. Actualizar el perfil (validando unicidad de nombreUsuario)
    @Transactional
    public UsuarioResponseDto actualizar(String uid, UsuarioRequestDto dto) {
        Usuario usuario = buscarEntidad(uid);

        // Si cambia el nombre de usuario, verificamos que no pertenezca a otra persona.
        // Si manda el mismo que ya tiene, no debe fallar.
        if (!usuario.getNombreUsuario().equals(dto.getNombreUsuario())
                && usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya está en uso");
        }

        // El correo y el id no se tocan: vienen de Firebase
        usuario.setNombre(dto.getNombre());
        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setFotoUrl(dto.getFotoUrl());

        return mapearADto(usuarioRepository.save(usuario));
    }

    // 3. Eliminar la cuenta: primero los datos, después Firebase.
    // Se divide en dos partes para que Firebase quede FUERA de la transacción:
    // si el borrado en la base falla, en Firebase no se toca nada.
    public void eliminar(String uid) {
        eliminarDatos(uid);

        // Si esto falla, los datos ya se borraron pero la cuenta sigue en Firebase.
        // Al volver a entrar, el filtro le crearía un registro nuevo y vacío.
        try {
            FirebaseAuth.getInstance().deleteUser(uid);
        } catch (FirebaseAuthException e) {
            log.error("Los datos de {} se borraron, pero falló el borrado en Firebase", uid, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "La cuenta se borró parcialmente, intenta de nuevo");
        }
        log.info("Cuenta eliminada por completo: {}", uid);
    }

    // --- Métodos internos ---

    // Borra los datos del usuario en el orden que permiten las llaves foráneas:
    // notas, luego materias, luego el usuario. Todo en una sola transacción.
    @Transactional
    protected void eliminarDatos(String uid) {
        if (!usuarioRepository.existsById(uid)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        notaRepository.deleteByUsuarioId(uid);
        materiaRepository.deleteByUsuarioId(uid);
        usuarioRepository.deleteById(uid);
    }

    // Busca el usuario o lanza 404
    private Usuario buscarEntidad(String uid) {
        return usuarioRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    // Genera un nombre de usuario único a partir de la parte antes de la @ del correo.
    // Si ya existe entonces, le va agregando números hasta encontrar uno libre.
    private String generarNombreUsuario(String correo) {
        String base = correo.contains("@")
                ? correo.substring(0, correo.indexOf("@"))
                : "usuario";

        // La columna admite máximo 40 caracteres; se deja margen para el número
        if (base.length() > 35) {
            base = base.substring(0, 35);
        }

        String nombreUsuario = base;
        int contador = 1;
        while (usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
            nombreUsuario = base + contador;
            contador++;
        }
        return nombreUsuario;
    }

    // Método auxiliar para mapear de Entidad a DTO
    private UsuarioResponseDto mapearADto(Usuario usuario) {
        return new UsuarioResponseDto(
                usuario.getId(),
                usuario.getCorreo(),
                usuario.getNombre(),
                usuario.getNombreUsuario(),
                usuario.getFotoUrl(),
                usuario.getFechaCreacion()
        );
    }
}