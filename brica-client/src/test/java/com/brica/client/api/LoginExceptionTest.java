package com.brica.client.api;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginExceptionTest {
    @Test
    void shouldUnwrapAsyncFailuresAndKeepSafeLoginMessage() {
        var failure = new CompletionException(new ExecutionException(new LoginException("Mensaje de acceso")));
        assertEquals("Mensaje de acceso", LoginException.messageFor(failure));
    }

    @Test
    void shouldIdentifyConnectionTimeoutAndTlsFailuresWithoutExposingDetails() {
        for (Exception failure : new Exception[] {
                new ConnectException("private-address"), new UnknownHostException("private-address") }) {
            String message = LoginException.messageFor(new CompletionException(failure));
            assertTrue(message.contains("No se pudo conectar"), message);
            assertFalse(message.contains("private-address"), message);
        }
        assertTrue(LoginException.messageFor(new HttpTimeoutException("private-address")).contains("no respondió a tiempo"));
        String tls = LoginException.messageFor(new SSLHandshakeException("private-certificate"));
        assertTrue(tls.contains("HTTPS"), tls);
        assertFalse(tls.contains("private-certificate"), tls);
    }

    @Test
    void shouldNeverDisplayArbitraryExceptionMessages() {
        String message = LoginException.messageFor(new RuntimeException("private-data"));
        assertFalse(message.contains("private-data"), message);
        assertFalse(message.contains("contraseña"), message);
    }
}
