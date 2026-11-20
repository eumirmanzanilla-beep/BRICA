package com.brica.server.shared.error.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ApiProblems {
    private ApiProblems() { }

    public static ProblemDetail problem(HttpStatus status, String code, String title,
            String detail, String suggestion) {
        var result = ProblemDetail.forStatusAndDetail(status, detail);
        result.setTitle(title);
        result.setProperty("code", code);
        result.setProperty("suggestion", suggestion);
        return result;
    }
}
