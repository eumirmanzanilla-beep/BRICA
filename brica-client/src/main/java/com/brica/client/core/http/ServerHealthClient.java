package com.brica.client.core.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ServerHealthClient {

    private final HttpClient httpClient;
    private final URI healthUri;

    public ServerHealthClient(URI serverBaseUri) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.healthUri = serverBaseUri.resolve("/api/v1/health");
    }

    public CompletableFuture<Boolean> isAvailable() {
        var request = HttpRequest.newBuilder()
                .uri(healthUri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(response -> response.statusCode() == 200);
    }
}
