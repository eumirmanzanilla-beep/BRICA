package com.brica.extraction.api;
import com.brica.extraction.application.ExtractionService;
import com.brica.extraction.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1")
public class ExtractionController {
    private final ExtractionService service;
    private final com.brica.extraction.application.MappingProfileService profiles;
    public ExtractionController(ExtractionService service,com.brica.extraction.application.MappingProfileService profiles) { this.service=service; this.profiles=profiles; }
    @GetMapping("/works/{work}/extraction-profiles") public List<com.brica.extraction.application.MappingProfileService.View> profiles(@PathVariable("work") Long work,@RequestParam(defaultValue="0",name="page") int page,@RequestParam(defaultValue="50",name="size") int size) { return profiles.list(work,page,size); }
    @PostMapping("/works/{work}/extraction-profiles") @ResponseStatus(HttpStatus.CREATED) public com.brica.extraction.application.MappingProfileService.View profile(@PathVariable("work") Long work,@RequestBody com.brica.extraction.application.MappingProfileService.Save request) { return profiles.save(work,request); }
    @GetMapping("/works/{work}/extraction-context") public ExtractionService.Context context(@PathVariable("work") Long work) { return service.context(work); }
    @GetMapping("/works/{work}/extractions") public List<ExtractionService.Summary> list(@PathVariable("work") Long work,@RequestParam(defaultValue="0",name="page") int page,@RequestParam(defaultValue="50",name="size") int size) { return service.list(work,page,size); }
    @PostMapping("/works/{work}/extractions") @ResponseStatus(HttpStatus.ACCEPTED) public ExtractionService.View create(@PathVariable("work") Long work,@RequestBody ExtractionService.Create request) { return service.create(work,request); }
    @GetMapping("/extractions/{id}") public ExtractionService.View get(@PathVariable("id") UUID id) { return service.get(id); }
    @GetMapping("/extractions/{id}/candidates") public List<SourceResult.Fragment> fragments(@PathVariable("id") UUID id,@RequestParam(defaultValue="0",name="page") int page,@RequestParam(defaultValue="100",name="size") int size) { return service.fragments(id,page,size); }
    @GetMapping("/extractions/{id}/suggestions") public List<com.brica.extraction.application.SourcePrecedencePolicy.Suggestion> suggestions(@PathVariable("id") UUID id,@RequestParam(defaultValue="0",name="page") int page,@RequestParam(defaultValue="100",name="size") int size) { return service.suggestions(id,page,size); }
    @PutMapping("/extractions/{id}/review") public ExtractionService.View save(@PathVariable("id") UUID id,@RequestHeader(value="If-Match",required=false) String expected,@RequestBody ExtractionReview review) { return service.save(id,expected,review); }
    @PostMapping("/extractions/{id}/confirmations") public ExtractionService.Confirmation confirm(@PathVariable("id") UUID id,@RequestHeader(value="If-Match",required=false) String expected,@RequestBody ExtractionService.Confirm request) { return service.confirm(id,expected,request); }
    @PostMapping("/extractions/{id}/cancellation") public ExtractionService.View cancel(@PathVariable("id") UUID id,@RequestHeader(value="If-Match",required=false) String expected) { return service.cancel(id,expected); }
    @PostMapping("/extractions/{id}/completion") public ExtractionService.View close(@PathVariable("id") UUID id,@RequestHeader(value="If-Match",required=false) String expected) { return service.close(id,expected); }
    public record Reanalyze(UUID operationKey) { }
    @GetMapping("/extractions/{id}/attempts") public List<ExtractionService.Attempt> attempts(@PathVariable("id") UUID id,@RequestParam(defaultValue="0",name="page") int page,@RequestParam(defaultValue="50",name="size") int size) { return service.attempts(id,page,size); }
    @PostMapping("/extractions/{id}/attempts") @ResponseStatus(HttpStatus.ACCEPTED)
    public ExtractionService.View reanalyze(@PathVariable("id") UUID id,@RequestBody Reanalyze request) { return service.reanalyze(id,request.operationKey()); }
}
