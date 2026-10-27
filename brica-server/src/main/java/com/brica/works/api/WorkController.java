package com.brica.works.api;

import java.util.List;

import com.brica.works.application.ListWorksService;
import com.brica.works.application.RegisterWorkService;
import com.brica.works.domain.Work;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/works")
public class WorkController {

    private final RegisterWorkService registerWorkService;
    private final ListWorksService listWorksService;

    public WorkController(
            RegisterWorkService registerWorkService,
            ListWorksService listWorksService) {

        this.registerWorkService = registerWorkService;
        this.listWorksService = listWorksService;
    }

    @GetMapping
    public List<WorkResponse> listWorks() {
        return listWorksService.listWorks()
                .stream()
                .map(WorkResponse::from)
                .toList();
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