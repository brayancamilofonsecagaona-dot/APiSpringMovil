package com.lecto.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

// Atrapa las excepciones de TODOS los controllers en un solo lugar.
// Así todos los errores salen con el mismo formato: {"error": "mensaje"},
// igual que los 401 del FirebaseTokenFilter.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Falla una validación del DTO (color inválido, nombre vacío) Responde con un 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Un mensaje por cada campo que falló, ej: {"color": "El color debe ser..."}
        // El nombre del campo se pasa a snake_case para que coincida con el resto del JSON
        Map<String, String> erroresCampos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> erroresCampos.put(aSnakeCase(error.getField()), error.getDefaultMessage()));

        // Map<String, Object> porque "campos" guarda otro mapa, no un texto
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("error", "Datos inválidos");
        respuesta.put("campos", erroresCampos);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    // JSON mal formado o un UUID invalido en el cuerpo responde con un 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la petición JSON está mal formado o es inválido");
    }

    // Un valor inválido en la URL, ej: GET /materias/abc (abc no es un UUID) responde con un 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "El valor de '" + ex.getName() + "' no es válido");
    }

    // Excepciones lanzadas desde el Service (404 Not found, 409 Conflict, etc)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        // Se respeta el código que puso el service: si lanzó 404, sale 404
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("error", ex.getReason() != null ? ex.getReason() : "Error en la petición");
        return ResponseEntity.status(ex.getStatusCode()).body(respuesta);
    }

    // La ruta no existe, ej: GET /materiaz responde 404
    // (sin este handler, el genérico de abajo lo convertiría en un 500)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "La ruta no existe");
    }

    // Método HTTP no permitido, ej: PATCH /materias responde 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " no permitido en esta ruta");
    }

    // Content-Type no soportado, ej: un POST sin "Content-Type: application/json" responde 415
    // (sin este handler, el genérico de abajo lo convertiría en un 500)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El tipo de contenido no es soportado, usa application/json");
    }

    // La base de datos rechace algo tipo una FK, integridad, etc, responde 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // Se loguea el detalle real para depurar, pero al cliente no se le muestra
        log.warn("Violación de integridad en la base de datos: {}", ex.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "Error de integridad en la base de datos");
    }

    // Excepcion de cualquier otra cosa inesperada, Responde 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // El stack trace completo va al log (en Render se ve en Logs)
        log.error("Error inesperado", ex);
        // A la persona, un mensaje genérico: nunca detalles internos
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno");
    }

    // metodo de ayuda o auxiliar

    // Arma la respuesta {"error": "mensaje"} para no repetir el mismo código en cada handler
    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("error", mensaje);
        return ResponseEntity.status(status).body(respuesta);
    }

    // Convierte un nombre de campo Java a snake_case, ej: fechaModificacion -> fecha_modificacion
    private String aSnakeCase(String campo) {
        return campo.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}