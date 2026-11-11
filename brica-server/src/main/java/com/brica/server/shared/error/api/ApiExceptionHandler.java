package com.brica.server.shared.error.api;

import java.util.LinkedHashMap;
import java.util.Map;

import com.brica.clients.application.ClientNotFoundException;
import com.brica.works.application.WorkNotFoundException;
import com.brica.server.shared.validation.domain.InvalidDataException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(com.brica.documents.domain.DocumentRuleException.class)
    public ProblemDetail documentRule(com.brica.documents.domain.DocumentRuleException exception) {
        return problem(HttpStatus.CONFLICT, exception.code(), "Configuración documental",
                exception.getMessage(), "Revisa el estado y la selección antes de continuar.");
    }
    @ExceptionHandler(com.brica.documents.application.DocumentException.class)
    public ProblemDetail document(com.brica.documents.application.DocumentException exception) {
        return problem(HttpStatus.valueOf(exception.status()), exception.code(), "Configuración documental",
                exception.getMessage(), "Revisa la selección o consulta su estado antes de reintentar.");
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ProblemDetail uploadTooLarge() {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "UPLOAD_TOO_LARGE", "Archivo demasiado grande",
                "La carga supera el límite configurado.", "Selecciona un DOCX más pequeño.");
    }
    @ExceptionHandler(ClientNotFoundException.class)
    public ProblemDetail clientNotFound(ClientNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "CLIENT_NOT_FOUND", "Cliente no encontrado",
                "No existe el cliente solicitado.", "Selecciona un cliente registrado.");
    }

    @ExceptionHandler(WorkNotFoundException.class)
    public ProblemDetail workNotFound(WorkNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "WORK_NOT_FOUND", "Obra no encontrada",
                "No existe la obra solicitada.", "Selecciona una obra registrada.");
    }

    @ExceptionHandler(InvalidDataException.class)
    public ProblemDetail invalidData(InvalidDataException exception) {
        return validation(exception.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidRequest(MethodArgumentNotValidException exception) {
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return validation(errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail malformedRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Solicitud inválida",
                "No se pudo interpretar la solicitud.", "Revisa el JSON, las fechas y los identificadores.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflict(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "DATA_CONFLICT", "Conflicto de datos",
                "No se pudo guardar la información.", "Actualiza los datos y comprueba sus relaciones.");
    }

    @ExceptionHandler(com.brica.works.application.StaleWorkException.class)
    public ProblemDetail stale(Exception exception) {
        return problem(HttpStatus.PRECONDITION_FAILED, "STALE_WORK", "La obra cambió",
                "La información fue modificada desde la consulta.", "Consulta la obra y revisa los cambios antes de guardar.");
    }

    @ExceptionHandler(com.brica.works.application.MissingPreconditionException.class)
    public ProblemDetail precondition(Exception exception) {
        return problem(HttpStatus.PRECONDITION_REQUIRED, "PRECONDITION_REQUIRED", "Falta condición de actualización",
                "La corrección requiere el encabezado If-Match.", "Consulta la obra y envía su ETag en If-Match.");
    }

    private ProblemDetail validation(Map<String, String> errors) {
        var result = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos inválidos",
                "Hay campos que requieren corrección.", "Corrige los campos indicados y vuelve a intentar.");
        result.setProperty("errors", errors);
        return result;
    }

    private ProblemDetail problem(HttpStatus status, String code, String title, String detail, String suggestion) {
        return ApiProblems.problem(status, code, title, detail, suggestion);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ProblemDetail authentication(jakarta.servlet.http.HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) { session.invalidate(); }
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return problem(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Inicia sesión",
                "No hay una sesión válida para esta operación.", "Inicia sesión con una cuenta activa.");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail accessDenied() {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Operación no autorizada",
                "Tu rol no tiene el permiso necesario.", "Solicita el permiso al administrador.");
    }

    @ExceptionHandler(com.brica.documents.application.DuplicateReferenceKeyException.class)
    public ProblemDetail duplicateReference() {
        return problem(HttpStatus.CONFLICT, "DUPLICATE_REFERENCE_KEY", "Clave duplicada",
                "La clave de referencia ya está registrada.", "Selecciona otra clave canónica.");
    }

    @ExceptionHandler(com.brica.documents.application.ReferenceFieldNotFoundException.class)
    public ProblemDetail referenceNotFound() {
        return problem(HttpStatus.NOT_FOUND, "REFERENCE_FIELD_NOT_FOUND", "Campo no encontrado",
                "No existe el campo solicitado.", "Actualiza la lista de campos.");
    }

    @ExceptionHandler(com.brica.documents.application.ReferenceSourceException.class)
    public ProblemDetail referenceSource() {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "REFERENCE_SOURCE_UNAVAILABLE", "Consulta no disponible",
                "No se pudieron leer los datos de origen.", "Vuelve a consultar antes de continuar.");
    }
}
