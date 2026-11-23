package com.brica.client.extraction.api;

import com.brica.client.clients.api.ClientsApiClient;
import com.brica.client.core.http.ServerSession;
import com.brica.client.extraction.api.ExtractionDtos.*;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class ExtractionApiClient {
    private final URI base; private final ServerSession session;
    private final ObjectMapper json=new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    public ExtractionApiClient(URI base) { this(base,ServerSession.shared(base)); }
    ExtractionApiClient(URI base,ServerSession session) { this.base=base; this.session=session; }
    private <T> CompletableFuture<T> request(String method,String path,Object body,String revision,Class<T> type) {
        try {
            var request=HttpRequest.newBuilder(base.resolve("/api/v1"+path)).timeout(Duration.ofSeconds(20));
            if(revision!=null) { request.header("If-Match",revision); }
            request.header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            return session.send(request.build()).thenApply(response->{
                try {
                    if(response.statusCode()<200 || response.statusCode()>=300) {
                        var error=json.readTree(response.body());
                        throw new ClientsApiClient.ApiException(response.statusCode(),error.path("detail").asText("Error HTTP "+response.statusCode())+"\n"+error.path("errors").toString());
                    }
                    return json.readValue(response.body(),type);
                } catch(com.fasterxml.jackson.core.JsonProcessingException failure) { throw new IllegalStateException("La respuesta del servidor no es válida.",failure); }
            });
        } catch(Exception failure) { return CompletableFuture.failedFuture(failure); }
    }
    public CompletableFuture<List<Job>> jobs(long work) { return request("GET","/works/"+work+"/extractions",null,null,Job[].class).thenApply(List::of); }
    public CompletableFuture<Context> context(long work) { return request("GET","/works/"+work+"/extraction-context",null,null,Context.class); }
    public CompletableFuture<View> create(long work,UUID operation,List<Long> sources) { return request("POST","/works/"+work+"/extractions",Map.of("operationKey",operation,"attachmentIds",sources),null,View.class); }
    public CompletableFuture<View> get(UUID id) { return request("GET","/extractions/"+id,null,null,View.class); }
    public CompletableFuture<View> reanalyze(UUID id,UUID operation) { return request("POST","/extractions/"+id+"/attempts",Map.of("operationKey",operation),null,View.class); }
    public CompletableFuture<List<Attempt>> attempts(UUID id) { return request("GET","/extractions/"+id+"/attempts?size=50",null,null,Attempt[].class).thenApply(List::of); }
    public CompletableFuture<List<Suggestion>> suggestions(UUID id) { return suggestions(id,0,new ArrayList<>()); }
    private CompletableFuture<List<Suggestion>> suggestions(UUID id,int page,List<Suggestion> values) {
        if(page>2000) { return CompletableFuture.failedFuture(new IllegalStateException("El resultado excede el límite de sugerencias.")); }
        return request("GET","/extractions/"+id+"/suggestions?page="+page+"&size=100",null,null,Suggestion[].class).thenCompose(rows->{
            values.addAll(List.of(rows));
            return rows.length<100?CompletableFuture.completedFuture(List.copyOf(values)):suggestions(id,page+1,values);
        });
    }
    public CompletableFuture<List<Fragment>> candidates(UUID id,int page) { return request("GET","/extractions/"+id+"/candidates?page="+page+"&size=100",null,null,Fragment[].class).thenApply(List::of); }
    public CompletableFuture<List<Fragment>> allCandidates(UUID id) { return candidates(id,0,new ArrayList<>()); }
    private CompletableFuture<List<Fragment>> candidates(UUID id,int page,List<Fragment> values) {
        if(page>2000) { return CompletableFuture.failedFuture(new IllegalStateException("El resultado excede el límite de candidatos.")); }
        return candidates(id,page).thenCompose(rows->{ values.addAll(rows); return rows.size()<100?CompletableFuture.completedFuture(List.copyOf(values)):candidates(id,page+1,values); });
    }
    public CompletableFuture<View> save(View view,Review review) { return request("PUT","/extractions/"+view.job().id()+"/review",review,view.job().revision(),View.class); }
    public CompletableFuture<Confirmation> confirm(View view,String target,UUID budget,UUID operation) {
        var body=new LinkedHashMap<String,Object>(); body.put("operationKey",operation); body.put("target",target); body.put("budgetBlockId",budget);
        return request("POST","/extractions/"+view.job().id()+"/confirmations",body,view.job().revision(),Confirmation.class);
    }
    public CompletableFuture<View> cancel(View view) { return request("POST","/extractions/"+view.job().id()+"/cancellation",null,view.job().revision(),View.class); }
    public CompletableFuture<View> complete(View view) { return request("POST","/extractions/"+view.job().id()+"/completion",null,view.job().revision(),View.class); }
    public CompletableFuture<List<BudgetView>> budgets(long work) { return request("GET","/works/"+work+"/budgets",null,null,BudgetView[].class).thenApply(List::of); }
    public CompletableFuture<List<BudgetRevision>> revisions(long budget) { return request("GET","/budgets/"+budget+"/revisions",null,null,BudgetRevision[].class).thenApply(List::of); }
    public CompletableFuture<List<Item>> items(UUID revision,int page) { return request("GET","/budget-revisions/"+revision+"/items?page="+page+"&size=100",null,null,Item[].class).thenApply(List::of); }
    public CompletableFuture<BudgetView> principal(BudgetView budget,UUID revision) { return request("PUT","/budgets/"+budget.id()+"/principal-revision",Map.of("revisionId",revision),budget.revision(),BudgetView.class); }
    public CompletableFuture<List<Profile>> profiles(long work) { return request("GET","/works/"+work+"/extraction-profiles",null,null,Profile[].class).thenApply(List::of); }
    public CompletableFuture<Profile> profile(long work,String name,Mapping mapping) { return request("POST","/works/"+work+"/extraction-profiles",Map.of("name",name,"mapping",mapping),null,Profile.class); }
}
