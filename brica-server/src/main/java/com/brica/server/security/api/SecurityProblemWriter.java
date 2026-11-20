package com.brica.server.security.api;

import java.io.IOException;

import com.brica.server.shared.error.api.ApiProblems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

public class SecurityProblemWriter {
    private final JsonMapper mapper;
    public SecurityProblemWriter(JsonMapper mapper) { this.mapper = mapper; }

    public void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var session = request.getSession(false);
        if (session != null) { session.invalidate(); }
        SecurityContextHolder.clearContext();
        write(response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Inicia sesión",
                "La sesión o las credenciales no son válidas.", "Inicia sesión con una cuenta activa.");
    }

    public void forbidden(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Operación no autorizada",
                "Falta autorización o la protección de la solicitud no es válida.",
                "Revisa tus permisos o vuelve a iniciar sesión antes de reenviar la solicitud.");
    }

    public void httpsRequired(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.BAD_REQUEST, "HTTPS_REQUIRED", "Conexión segura requerida",
                "Esta operación requiere HTTPS.", "Configura el servidor y el cliente con una URL HTTPS.");
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String title,
            String detail, String suggestion) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ApiProblems.problem(status, code, title, detail, suggestion));
    }
}
