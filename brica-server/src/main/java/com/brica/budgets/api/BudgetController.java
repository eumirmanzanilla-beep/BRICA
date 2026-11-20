package com.brica.budgets.api;
import com.brica.budgets.application.BudgetService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1")
public class BudgetController {
    private final BudgetService service;
    public BudgetController(BudgetService service) { this.service=service; }
    @GetMapping("/works/{work}/budgets") public List<BudgetService.View> list(@PathVariable("work") Long work,@RequestParam(defaultValue="0",name="page") int page,@RequestParam(defaultValue="50",name="size") int size) { return service.list(work,page,size); }
    @GetMapping("/budgets/{id}/revisions") public List<BudgetService.Revision> revisions(@PathVariable("id") Long id,@RequestParam(defaultValue="0",name="page") int page,@RequestParam(defaultValue="50",name="size") int size) { return service.revisions(id,page,size); }
    @GetMapping("/budget-revisions/{id}/sections") public List<BudgetService.Section> sections(@PathVariable("id") UUID id) { return service.sections(id); }
    @GetMapping("/budget-revisions/{id}/items") public List<BudgetService.Item> items(@PathVariable("id") UUID id,@RequestParam(defaultValue="0",name="page") int page,@RequestParam(defaultValue="50",name="size") int size) { return service.items(id,page,size); }
    public record Principal(UUID revisionId) { }
    @PutMapping("/budgets/{id}/principal-revision") public BudgetService.View principal(@PathVariable("id") Long id,@RequestHeader(value="If-Match",required=false) String expected,@RequestBody Principal request) { return service.principal(id,request.revisionId(),expected); }
}
