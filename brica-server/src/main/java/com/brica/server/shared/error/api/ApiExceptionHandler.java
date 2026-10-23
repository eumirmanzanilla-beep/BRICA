package com.brica.server.shared.error.api;

import com.brica.clientes.application.ClientNotFoundException;

import com.brica.works.application.WorkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ClientNotFoundException.class)
    public ProblemDetail handleClientNotFound(
            ClientNotFoundException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.getMessage()
        );

        problem.setTitle("Client not found");

        return problem;
    }

    @ExceptionHandler(WorkNotFoundException.class)
        public ProblemDetail handleWorkNotFound(
                WorkNotFoundException exception) {

            ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    exception.getMessage()
                );

            problem.setTitle("Work not found");

            return problem;
        }
}