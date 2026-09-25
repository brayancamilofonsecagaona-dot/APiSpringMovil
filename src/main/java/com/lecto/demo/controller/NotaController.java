package com.lecto.demo.controller;

import com.lecto.demo.dto.NotaRequestDto;
import com.lecto.demo.dto.NotaResponseDto;
import com.lecto.demo.service.NotaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notas")
public class NotaController {

    private final NotaService notaService;

    public NotaController(NotaService notaService) {
        this.notaService = notaService;
    }

    // Método auxiliar para extraer el UID del usuario autenticado
    private String obtenerUid(HttpServletRequest request) {
        return (String) request.getAttribute("uid");
    }

    // 1. GET /notas - Listar notas (con filtros opcionales por materia y texto de búsqueda)
    @GetMapping
    public ResponseEntity<List<NotaResponseDto>> listar(
            @RequestParam(name = "materia_id", required = false) UUID materiaId,
            @RequestParam(required = false) String q,
            HttpServletRequest request) {
        String uid = obtenerUid(request);
        List<NotaResponseDto> notas = notaService.listar(uid, materiaId, q);
        return ResponseEntity.ok(notas);
    }

    // 2. GET /notas/{id} - Obtener una nota por su ID
    @GetMapping("/{id}")
    public ResponseEntity<NotaResponseDto> obtener(@PathVariable UUID id, HttpServletRequest request) {
        String uid = obtenerUid(request);
        NotaResponseDto nota = notaService.obtener(id, uid);
        return ResponseEntity.ok(nota);
    }

    // 3. POST /notas - Crear una nueva nota
    @PostMapping
    public ResponseEntity<NotaResponseDto> crear(@Valid @RequestBody NotaRequestDto dto,
                                                 HttpServletRequest request) {
        String uid = obtenerUid(request);
        NotaResponseDto creada = notaService.crear(dto, uid);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // 4. PUT /notas/{id} - Actualizar una nota existente
    @PutMapping("/{id}")
    public ResponseEntity<NotaResponseDto> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody NotaRequestDto dto,
            HttpServletRequest request) {
        String uid = obtenerUid(request);
        NotaResponseDto actualizada = notaService.actualizar(id, dto, uid);
        return ResponseEntity.ok(actualizada);
    }

    // 5. DELETE /notas/{id} - Borrado lógico de una nota
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, HttpServletRequest request) {
        String uid = obtenerUid(request);
        notaService.eliminar(id, uid);
        return ResponseEntity.noContent().build(); // Devuelve código 204 No Content
    }
}