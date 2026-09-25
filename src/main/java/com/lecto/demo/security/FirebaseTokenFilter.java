package com.lecto.demo.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.lecto.demo.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenFilter.class);
    private static final String PREFIJO_BEARER = "Bearer ";

    private final UsuarioService usuarioService;

    // Constructor para inyectar el UsuarioService
    public FirebaseTokenFilter(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // 1. El header debe existir y traer el prefijo Bearer
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            responderError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token ausente o mal formado");
            return;
        }

        String idToken = header.substring(PREFIJO_BEARER.length());
        String uid;
        FirebaseToken token;

        // 2. El try cubre SOLO la verificación del token, nunca la cadena de filtros
        try {
            token = FirebaseAuth.getInstance().verifyIdToken(idToken);
            uid = token.getUid();

        } catch (FirebaseAuthException e) {
            // Token vencido, de otro proyecto, firma inválida, etc.
            log.warn("Token inválido: {}", e.getMessage());
            responderError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o expirado");
            return;

        } catch (Exception e) {
            // Fallo del lado del servidor, por ejemplo Firebase sin inicializar
            log.error("Error inesperado validando el token", e);
            responderError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno de autenticación");
            return;
        }

        // 3. Aseguramos que el usuario exista en la base de datos de manera automática.
        // Va en su propio try: los filtros corren antes de Spring MVC, así que el
        // @RestControllerAdvice no atraparía este error y saldría la página de Tomcat.
        try {
            usuarioService.asegurarUsuario(token);
        } catch (Exception e) {
            log.error("No se pudo registrar el usuario {}", uid, e);
            responderError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "No se pudo registrar el usuario");
            return;
        }

        // 4. Token válido: el uid queda disponible para los controllers
        request.setAttribute("uid", uid);

        // 5. doFilter va al final y FUERA de los try, para no capturar errores de los controllers
        filterChain.doFilter(request, response);
    }

    /** Centraliza las respuestas de error del filtro en formato JSON. */
    private void responderError(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");


    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // /health queda público y el preflight de CORS llega sin token
        return request.getRequestURI().startsWith("/health")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}

