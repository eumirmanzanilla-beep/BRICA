package com.brica.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExtractionApiClientTest {
    @Test void readsSuggestionsAndAttemptsAndUsesCallerOperationKeyForReanalysis() throws Exception {
        var job=UUID.randomUUID();
        var child=UUID.randomUUID();
        var fragment=UUID.randomUUID();
        var operation=UUID.randomUUID();
        var sent=new AtomicReference<String>();
        var csrf=new AtomicReference<String>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            String path=exchange.getRequestURI().getPath();
            String prefix="/api/v1/extractions/"+job;
            String response;
            int status=200;
            if(path.endsWith("/csrf")) {
                response="{\"headerName\":\"X-CSRF-TOKEN\",\"token\":\"proof\"}";
            } else if(path.endsWith("/login")) {
                response="{\"id\":1,\"username\":\"Reviewer\",\"name\":\"Reviewer\",\"roleId\":1,\"roleName\":\"Reviewer\",\"enabled\":true,\"permissions\":[\"WORKS_READ\",\"WORKS_WRITE\"]}";
            } else if(path.equals(prefix+"/suggestions") && exchange.getRequestMethod().equals("GET")) {
                response=("""
                        [{"fragmentId":"%s","attachmentId":12,"target":"WORK_CONTRACT_NUMBER",
                          "proposedValue":"0012","recommended":false,"reason":"Conflicto"}]
                        """).formatted(fragment);
            } else if(path.equals(prefix+"/attempts") && exchange.getRequestMethod().equals("GET")) {
                response="[{\"number\":1,\"outcome\":\"FAILED\",\"diagnostic\":\"Sin lector disponible\"}]";
            } else if(path.equals(prefix+"/attempts") && exchange.getRequestMethod().equals("POST")) {
                sent.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
                csrf.set(exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN"));
                status=202;
                response="{\"job\":{\"id\":\""+child+"\",\"state\":\"QUEUED\"}}";
            } else {
                status=404; response="{\"detail\":\"Ruta no prevista\"}";
            }
            try(exchange) {
                var bytes=response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status,bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        try {
            var base=URI.create("http://127.0.0.1:"+server.getAddress().getPort());
            var session=new ServerSession(base,true);
            session.login("Reviewer","Synthetic123").join();
            var api=new ExtractionApiClient(base,session);
            var suggestion=api.suggestions(job).join().getFirst();
            assertEquals(fragment,suggestion.fragmentId());
            assertEquals("0012",suggestion.proposedValue());
            assertFalse(suggestion.recommended());
            assertEquals("Sin lector disponible",api.attempts(job).join().getFirst().diagnostic());
            assertEquals(child,api.reanalyze(job,operation).join().job().id());
            assertEquals(operation.toString(),new ObjectMapper().readTree(sent.get()).path("operationKey").asText());
            assertEquals("proof",csrf.get());
        } finally { server.stop(0); }
    }
}
