package com.brica.works.api;

import com.brica.works.application.RegisterWorkService;
import com.brica.works.domain.Work;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/works")
public class WorkController {

    private final RegisterWorkService registerWorkService;

    public WorkController(
            RegisterWorkService registerWorkService) {

        this.registerWorkService = registerWorkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkResponse registerWork(
            @Valid @RequestBody RegisterWorkRequest request) {

        Work work = registerWorkService.registerWork(
            request.clientId(),
            request.name(),
            request.location(),
            request.description(),
            request.startDate(),
            request.endDate()
        );

        return WorkResponse.from(work);
    }
}   