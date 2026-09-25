package com.lecto.demo.controller;

import com.lecto.demo.dto.SyncResponseDto;
import com.lecto.demo.service.SyncService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

// Endpoint que usa la app para mantener alineada su base local
@RestController
@RequestMapping("/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    // Método auxiliar para extraer el UID del usuario autenticado
    private String obtenerUid(HttpServletRequest request) {
        return (String) request.getAttribute("uid");
    }

    // GET
    // Sin el parámetro devuelve todo lo activo (primera sincronización)
    @GetMapping
    public ResponseEntity<SyncResponseDto> sincronizar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            HttpServletRequest request) {
        String uid = obtenerUid(request);
        return ResponseEntity.ok(syncService.sincronizar(uid, desde));
    }
}