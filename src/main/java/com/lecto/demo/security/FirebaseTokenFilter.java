package com.lecto.demo.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // 1. El header debe existir y traer el prefijo Bearer
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            responderNoAutorizado(response, "Token ausente o mal formado");

            return;
        }

        String idToken = header.substring(PREFIJO_BEARER.length());
        String uid;

        // 2. El try cubre SOLO la verificación del token, nunca la cadena de filtros
        try {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
            uid = token.getUid();

        } catch (FirebaseAuthException e) {
            // Token vencido, de otro proyecto, firma inválida, etc.
            log.warn("Token inválido: {}", e.getMessage());
            responderNoAutorizado(response, "Token inválido o expirado");
            return;

        } catch (Exception e) {
            // Fallo del lado del servidor, por ejemplo Firebase sin inicializar
            log.error("Error inesperado validando el token", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // 3. Token válido: el uid queda disponible para los controllers
        request.setAttribute("uid", uid);

        // 4. doFilter va al final y FUERA del try, para no capturar errores de los controllers
        filterChain.doFilter(request, response);
    }

    /** Centraliza la respuesta 401 en formato JSON. */

    private void responderNoAutorizado(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
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