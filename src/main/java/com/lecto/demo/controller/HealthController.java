package com.lecto.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    // Logger para dejar rastro en consola (y en los logs de Render) cuando algo falla
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        // LinkedHashMap respeta el orden de inserción en el JSON
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "up"); // si llegamos aquí, la app está viva


        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            body.put("db", "up");
            return ResponseEntity.ok(body);

        } catch (DataAccessException e) {
            // Solo capturamos errores de acceso a datos, no cualquier excepción
            log.error("Health check: no hay conexión con la base de datos", e);
            body.put("status", "degraded"); // la app responde, pero sin base de datos
            body.put("db", "down");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }
}