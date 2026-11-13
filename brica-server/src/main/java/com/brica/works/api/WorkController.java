
package com.brica.works.api;

import java.util.List;

import com.brica.works.application.ConsultWorkService;
import com.brica.works.application.CorrectWorkService;
import com.brica.works.application.DeleteWorkService;
import com.brica.works.application.ListWorksService;
import com.brica.works.application.RegisterWorkService;
import com.brica.works.application.WorkRevision;
import com.brica.works.domain.Work;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/works")
public class WorkController {

    private final RegisterWorkService registerWorkService;
    private final ListWorksService listWorksService;
    private final ConsultWorkService consultService;
    private final CorrectWorkService correctService;
    private final DeleteWorkService deleteWorkService;
    private final com.brica.works.application.DocumentInformationService documentInformationService;

    public WorkController(
            RegisterWorkService registerWorkService,
            ListWorksService listWorksService,
            ConsultWorkService consultService,
            CorrectWorkService correctService,
            DeleteWorkService deleteWorkService,
            com.brica.works.application.DocumentInformationService documentInformationService) {

        this.registerWorkService = registerWorkService;
        this.listWorksService = listWorksService;
        this.consultService = consultService;
        this.correctService = correctService;
        this.deleteWorkService = deleteWorkService;
        this.documentInformationService = documentInformationService;
    }

    @GetMapping
    public List<WorkResponse> listWorks(
            @RequestParam(value = "page", required = false)
            Integer page,

            @RequestParam(value = "size", required = false)
            Integer size) {

        List<Work> works;

        if (page == null && size == null) {
            works = listWorksService.listWorks();
        } else {
            works = listWorksService.listWorks(
                    page == null ? 0 : page,
                    size == null ? 50 : size
            );
        }

        return works.stream()
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
    public ResponseEntity<WorkResponse> get(
            @PathVariable("id") Long id) {

        Work work = consultService.get(id);

        return ResponseEntity.ok()
                .eTag(WorkRevision.etag(work))
                .body(WorkResponse.from(work));
    }

    @PutMapping("/{id}/general-information")
    public ResponseEntity<WorkResponse> correct(
            @PathVariable("id") Long id,
            @RequestHeader(
                    value = "If-Match",
                    required = false
            ) String expectedEtag,
            @Valid @RequestBody CorrectWorkRequest request) {

        Work work = correctService.correct(
                id,
                expectedEtag,
                request.name(),
                request.location(),
                request.description()
        );

        return ResponseEntity.ok()
                .eTag(WorkRevision.etag(work))
                .body(WorkResponse.from(work));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
        public void deleteWork(@PathVariable("id") Long id) {
            deleteWorkService.delete(id);
        }

    @PutMapping("/{id}/document-information")
    public ResponseEntity<DocumentInformationResponse> updateDocumentInformation(@PathVariable("id") Long id,
            @RequestHeader(value = "If-Match", required = false) String expected,
            @Valid @RequestBody com.brica.works.application.DocumentInformationService.Information request) {
        var work = documentInformationService.update(id, expected, request);
        return ResponseEntity.ok().eTag(WorkRevision.etag(work)).body(DocumentInformationResponse.from(work));
    }

    @GetMapping("/{id}/document-information")
    public ResponseEntity<DocumentInformationResponse> documentInformation(@PathVariable("id") Long id) {
        var work = consultService.get(id);
        return ResponseEntity.ok().eTag(WorkRevision.etag(work)).body(DocumentInformationResponse.from(work));
    }
    @GetMapping("/{id}/information")
    public ResponseEntity<CompleteResponse> information(@PathVariable("id") Long id) {
        var work = consultService.get(id);
        return ResponseEntity.ok().eTag(WorkRevision.etag(work)).body(CompleteResponse.from(work));
    }
    @PutMapping("/{id}/information")
    public ResponseEntity<CompleteResponse> updateInformation(@PathVariable("id") Long id,
            @RequestHeader(value = "If-Match", required = false) String etag,
            @RequestBody com.brica.works.application.DocumentInformationService.CompleteInformation information) {
        var work = documentInformationService.updateAll(id, etag, information);
        return ResponseEntity.ok().eTag(WorkRevision.etag(work)).body(CompleteResponse.from(work));
    }
    public record CompleteResponse(WorkResponse work, DocumentInformationResponse documentInformation) {
        static CompleteResponse from(Work work) { return new CompleteResponse(WorkResponse.from(work), DocumentInformationResponse.from(work)); }
    }
}
