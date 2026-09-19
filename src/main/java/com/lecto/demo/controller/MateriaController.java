package com.lecto.demo.controller;

import com.lecto.demo.dto.MateriaRequestDto;
import com.lecto.demo.dto.MateriaResponseDto;
import com.lecto.demo.service.MateriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/materias")
public class MateriaController {

    private final MateriaService materiaService;

    public MateriaController(MateriaService materiaService) {
        this.materiaService = materiaService;
    }

    // GET /materias : Lista todas las materias del usuario autenticado
    @GetMapping
    public ResponseEntity<List<MateriaResponseDto>> listar(HttpServletRequest request) {
        return ResponseEntity.ok(materiaService.listar(uid(request)));
    }

    // GET /materias/{id}: Obtiene el detalle de una materia
    @GetMapping("/{id}")
    public ResponseEntity<MateriaResponseDto> obtener(@PathVariable UUID id, HttpServletRequest request) {
        return ResponseEntity.ok(materiaService.obtener(id, uid(request)));
    }

    // POST /materias : Crea una nueva materia (retorna 201 Created)
    @PostMapping
    public ResponseEntity<MateriaResponseDto> crear(@Valid @RequestBody MateriaRequestDto dto,
                                                    HttpServletRequest request) {
        MateriaResponseDto creada = materiaService.crear(dto, uid(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // PUT /materias/{id} : Actualiza una materia existente.
    // El id que manda es el de la URL; si el body trae otro, se ignora.
    @PutMapping("/{id}")
    public ResponseEntity<MateriaResponseDto> actualizar(@PathVariable UUID id,
                                                         @Valid @RequestBody MateriaRequestDto dto,
                                                         HttpServletRequest request) {
        return ResponseEntity.ok(materiaService.actualizar(id, dto, uid(request)));
    }

    // DELETE /materias/{id} : Borrado retorna 204
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, HttpServletRequest request) {
        materiaService.eliminar(id, uid(request));
        return ResponseEntity.noContent().build();
    }

    // El filtro de Firebase guarda el uid del token en el request; aquí se lee.
    private String uid(HttpServletRequest request) {
        return (String) request.getAttribute("uid");
    }
}