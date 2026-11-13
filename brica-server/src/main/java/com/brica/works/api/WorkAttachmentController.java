package com.brica.works.api;

import java.io.IOException;
import java.util.List;
import com.brica.works.application.WorkAttachmentService;
import com.brica.works.domain.WorkAttachment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/works/{workId}/attachments")
public class WorkAttachmentController {
    private final WorkAttachmentService service;
    public WorkAttachmentController(WorkAttachmentService service) { this.service = service; }
    @GetMapping
    public List<WorkAttachmentService.View> list(@PathVariable("workId") Long workId,
            @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "50") int size) {
        return service.list(workId, page, size);
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public WorkAttachmentService.View upload(@PathVariable("workId") Long workId, @RequestParam("kind") WorkAttachment.Kind kind,
            @RequestPart("file") MultipartFile file) throws IOException {
        return service.upload(workId, kind, file.getOriginalFilename(), file.getBytes());
    }
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> download(@PathVariable("workId") Long workId, @PathVariable("id") Long id) {
        var file = service.download(workId, id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.name(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }
}
