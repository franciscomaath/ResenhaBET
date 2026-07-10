package com.franciscomaath.resenhaapi.config;

import com.franciscomaath.resenhaapi.domain.entity.Session;
import com.franciscomaath.resenhaapi.domain.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SessionFilter extends OncePerRequestFilter {

    private final SessionRepository sessionRepository;
    private final CurrentUserContext currentUserContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isPublicRoute(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response, "Token de sessao ausente.");
            return;
        }

        try {
            UUID token = UUID.fromString(authorization.substring("Bearer ".length()).trim());
            Session session = sessionRepository.findByTokenWithUser(token)
                    .filter(activeSession -> activeSession.getExpiresAt().isAfter(LocalDateTime.now()))
                    .orElse(null);

            if (session == null) {
                writeUnauthorized(response, "Token de sessao invalido ou expirado.");
                return;
            }

            currentUserContext.set(session.getUser(), session.getCurrentGroup(), token);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            writeUnauthorized(response, "Token de sessao invalido.");
        } finally {
            currentUserContext.clear();
        }
    }

    private boolean isPublicRoute(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        return ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/auth/login"))
                || (("GET".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method)) && path.equals("/api/v1/users"))
                || ("GET".equalsIgnoreCase(method) && path.startsWith("/api/v1/teams"))
                || path.startsWith("/ws")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}
