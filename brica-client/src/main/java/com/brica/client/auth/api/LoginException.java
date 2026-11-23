package com.brica.client.auth.api;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLException;

public final class LoginException extends RuntimeException {
    public LoginException(String message) { super(message); }

    public static String messageFor(Throwable failure) {
        while ((failure instanceof CompletionException || failure instanceof ExecutionException)
                && failure.getCause() != null) {
            failure = failure.getCause();
        }
        if (failure instanceof LoginException) { return failure.getMessage(); }
        if (failure instanceof HttpTimeoutException) {
            return "El servidor no respondió a tiempo. Comprueba la conexión y vuelve a intentar.";
        }
        if (failure instanceof ConnectException || failure instanceof UnknownHostException) {
            return "No se pudo conectar con el servidor. Comprueba que esté iniciado y revisa BRICA_SERVER_URL.";
        }
        if (failure instanceof SSLException) {
            return "No se pudo establecer HTTPS. Revisa el certificado y la configuración TLS del servidor.";
        }
        return "No se pudo completar el acceso. Vuelve a intentar; si persiste, revisa el servidor.";
    }
}
