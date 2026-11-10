package com.brica.client.config;

import java.net.URI;

public final class ClientConfig {

    private static final String DEFAULT_SERVER_URL = "http://localhost:8080";

    private ClientConfig() {
    }

    public static boolean allowInsecureHttp() {
        return Boolean.parseBoolean(System.getenv("BRICA_ALLOW_HTTP"));
    }

    public static URI serverBaseUri() {
        var configuredUrl = System.getProperty("brica.server.url");

        if (configuredUrl == null || configuredUrl.isBlank()) {
            configuredUrl = System.getenv("BRICA_SERVER_URL");
        }

        if (configuredUrl == null || configuredUrl.isBlank()) {
            configuredUrl = DEFAULT_SERVER_URL;
        }

        return URI.create(configuredUrl);
    }
}
