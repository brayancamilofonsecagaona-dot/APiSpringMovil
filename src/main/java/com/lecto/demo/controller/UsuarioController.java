package com.lecto.demo.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.lecto.demo.dto.UsuarioRequestDto;
import com.lecto.demo.dto.UsuarioResponseDto;
import com.lecto.demo.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController // Con esta anotación le decimos a Spring que va a manejar peticiones web
@RequestMapping("/usuarios") // Prefijo base para todos los endpoints de este controlador
public class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    private final UsuarioService usuarioService;

    // Inyección de dependencias por constructor
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Método auxiliar para extraer el UID del request que guardó el filtro de seguridad
    private String obtenerUid(HttpServletRequest request) {
        return (String) request.getAttribute("uid");
    }

    // 1. GET /usuarios/me - Obtener el perfil propio
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDto> obtenerPerfil(HttpServletRequest request) {
        String uid = obtenerUid(request);
        UsuarioResponseDto perfil = usuarioService.obtener(uid);
        return ResponseEntity.ok(perfil); // Devuelve código 200 con el perfil
    }

    // 2. POST /usuarios - Registrar/Confirmar usuario (el filtro ya lo creó, aquí solo devolvemos el perfil con 201)
    @PostMapping
    public ResponseEntity<UsuarioResponseDto> crearRegistro(HttpServletRequest request) {
        String uid = obtenerUid(request);
        UsuarioResponseDto perfil = usuarioService.obtener(uid);
        return ResponseEntity.status(HttpStatus.CREATED).body(perfil); // Devuelve código 201 Created
    }

    // 3. PUT /usuarios/me - Actualizar el perfil (con validaciones del DTO)
    @PutMapping("/me")
    public ResponseEntity<UsuarioResponseDto> actualizarPerfil(@Valid @RequestBody UsuarioRequestDto dto,
                                                               HttpServletRequest request) {
        String uid = obtenerUid(request);
        UsuarioResponseDto actualizado = usuarioService.actualizar(uid, dto);
        return ResponseEntity.ok(actualizado); // Devuelve código 200 con el perfil actualizado
    }

    // 4. DELETE /usuarios/me - Eliminar la cuenta por completo
    @DeleteMapping("/me")
    public ResponseEntity<Void> eliminarCuenta(HttpServletRequest request) {
        String uid = obtenerUid(request);

        // Primero los datos: el service los borra en una sola transacción
        usuarioService.eliminar(uid);

        // Después Firebase, cuando la transacción ya terminó.
        // Si esto falla, los datos ya se borraron pero la cuenta sigue en Firebase.
        // Al volver a entrar, el filtro le crearía un registro nuevo y vacío.
        try {
            FirebaseAuth.getInstance().deleteUser(uid);
        } catch (FirebaseAuthException e) {
            log.error("Los datos de {} se borraron, pero falló el borrado en Firebase", uid, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Los datos de la cuenta ya se borraron, pero no se pudo eliminar el usuario en Firebase");
        }
        log.info("Cuenta eliminada por completo: {}", uid);

        return ResponseEntity.noContent().build(); // Devuelve código 204 No Content sin cuerpo
    }
}