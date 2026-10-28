package com.brica.works.api;

import java.util.List;
import com.brica.works.application.ConsultWorkService;
import com.brica.works.application.CorrectWorkService;
import com.brica.works.application.WorkRevision;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

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
    private final ConsultWorkService consultService;
    private final CorrectWorkService correctService;

    public WorkController(
            RegisterWorkService registerWorkService,
            ListWorksService listWorksService,
            ConsultWorkService consultService,
            CorrectWorkService correctService) {

        this.registerWorkService = registerWorkService;
        this.listWorksService = listWorksService;
        this.consultService = consultService;
        this.correctService = correctService;
    }

    @GetMapping
    public List<WorkResponse> listWorks(
            @org.springframework.web.bind.annotation.RequestParam(value = "page", required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(value = "size", required = false) Integer size) {
        return (page == null && size == null ? listWorksService.listWorks()
                : listWorksService.listWorks(page == null ? 0 : page, size == null ? 50 : size))
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
    @GetMapping("/{id}")
    public ResponseEntity<WorkResponse> get(@PathVariable("id") Long id) {
        var work = consultService.get(id);
        return ResponseEntity.ok().eTag(WorkRevision.etag(work)).body(WorkResponse.from(work));
    }

    @PutMapping("/{id}/general-information")
    public ResponseEntity<WorkResponse> correct(@PathVariable("id") Long id,
            @RequestHeader(value = "If-Match", required = false) String expectedEtag,
            @Valid @RequestBody CorrectWorkRequest request) {
        var work = correctService.correct(id, expectedEtag, request.name(), request.location(), request.description());
        return ResponseEntity.ok().eTag(WorkRevision.etag(work)).body(WorkResponse.from(work));
    }
}
