package com.brica.contracts.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(
        named = "brica.docker.integration",
        matches = "true"
)
class ContractDockerHttpIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void shouldRegisterContractOverHttpAndPersistJsonb() throws Exception {
        String baseUrl = environmentOrDefault(
                "BRICA_BASE_URL",
                "http://localhost:8080"
        ).replaceAll("/+$", "");

        String contractNumber = "BRICA-HTTP-IT-" + UUID.randomUUID();
        String invalidContractNumber = contractNumber + "-INVALID";

        String databaseUrl = getDatabaseUrl();
        String databaseUser = requiredEnvironmentVariable(
                "BRICA_TEST_DB_USERNAME",
                "POSTGRES_USER"
        );
        String databasePassword = requiredEnvironmentVariable(
                "BRICA_TEST_DB_PASSWORD",
                "POSTGRES_PASSWORD"
        );

        try (Connection connection = DriverManager.getConnection(
                databaseUrl,
                databaseUser,
                databasePassword
        )) {
            Throwable testFailure = null;

            try {
                // 1. Verificar que responde el backend desplegado.
                HttpRequest healthRequest = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/v1/health"))
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> healthResponse = httpClient.send(
                        healthRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

                assertEquals(
                        200,
                        healthResponse.statusCode(),
                        "El health endpoint no respondió correctamente: "
                                + healthResponse.body()
                );

                // 2. Obtener una obra existente de la base de datos de Compose.
                long workId = findExistingWorkId(connection);

                // 3. Preparar datos representativos para probar JSONB.
                Map<String, Object> contractData = Map.of(
                        "contractNumber", contractNumber,
                        "amount", 123456.78,
                        "requiresReview", true,
                        "metadata", Map.of(
                                "source", "docker-compose",
                                "verified", true
                        )
                );

                JsonNode expectedData = JSON.readTree(
                        JSON.writeValueAsString(contractData)
                );

                Map<String, Object> requestBody = Map.of(
                        "workId", workId,
                        "data", contractData
                );

                HttpResponse<String> createResponse = postJson(
                        baseUrl,
                        "/api/v1/contracts",
                        requestBody
                );

                assertEquals(
                        201,
                        createResponse.statusCode(),
                        "La API no registró el contrato. Respuesta: "
                                + createResponse.body()
                );

                JsonNode responseBody = JSON.readTree(
                        createResponse.body()
                );

                assertTrue(
                        responseBody.hasNonNull("id"),
                        "La respuesta no incluye el ID del contrato."
                );

                assertEquals(
                        workId,
                        responseBody.path("workId").asLong(),
                        "La respuesta contiene una obra diferente."
                );

                assertEquals(
                        expectedData,
                        responseBody.path("data"),
                        "Los datos devueltos no coinciden con los enviados."
                );

                // 4. Consultar PostgreSQL para verificar la persistencia real.
                assertContractPersisted(
                        connection,
                        contractNumber,
                        workId,
                        expectedData
                );

                // 5. Verificar que una obra inexistente devuelve HTTP 404.
                HttpResponse<String> invalidResponse = postJson(
                        baseUrl,
                        "/api/v1/contracts",
                        Map.of(
                                "workId", Long.MAX_VALUE,
                                "data", Map.of(
                                        "contractNumber",
                                        invalidContractNumber
                                )
                        )
                );

                assertEquals(
                        404,
                        invalidResponse.statusCode(),
                        "Se esperaba HTTP 404 para una obra inexistente. "
                                + "Respuesta: " + invalidResponse.body()
                );

                assertContractDoesNotExist(
                        connection,
                        invalidContractNumber
                );

            } catch (Exception | AssertionError failure) {
                testFailure = failure;
                throw failure;
            } finally {
                // Limpiar únicamente los registros creados por esta prueba.
                try {
                    deleteContractByNumber(connection, contractNumber);
                    deleteContractByNumber(
                            connection,
                            invalidContractNumber
                    );
                } catch (SQLException cleanupFailure) {
                    if (testFailure != null) {
                        testFailure.addSuppressed(cleanupFailure);
                    } else {
                        throw cleanupFailure;
                    }
                }
            }
        }
    }

    private HttpResponse<String> postJson(
            String baseUrl,
            String path,
            Object body
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        JSON.writeValueAsString(body)
                ))
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private static long findExistingWorkId(
            Connection connection
    ) throws SQLException {
        String sql = """
                SELECT id_work
                FROM works
                ORDER BY id_work DESC
                LIMIT 1
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            if (!resultSet.next()) {
                throw new AssertionError(
                        "No hay obras registradas en PostgreSQL. "
                                + "Registra una obra antes de ejecutar "
                                + "esta prueba."
                );
            }

            return resultSet.getLong("id_work");
        }
    }

    private static void assertContractPersisted(
            Connection connection,
            String contractNumber,
            long expectedWorkId,
            JsonNode expectedData
    ) throws Exception {
        String sql = """
                SELECT work_id, data::text
                FROM contracts
                WHERE data ->> 'contractNumber' = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setString(1, contractNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(
                        resultSet.next(),
                        "El contrato no fue encontrado en PostgreSQL."
                );

                assertEquals(
                        expectedWorkId,
                        resultSet.getLong("work_id"),
                        "El contrato quedó asociado a otra obra."
                );

                JsonNode persistedData = JSON.readTree(
                        resultSet.getString("data")
                );

                assertEquals(
                        expectedData,
                        persistedData,
                        "El JSONB persistido no coincide con el esperado."
                );

                assertFalse(
                        resultSet.next(),
                        "La marca de la prueba debería identificar "
                                + "un único contrato."
                );
            }
        }
    }

    private static void assertContractDoesNotExist(
            Connection connection,
            String contractNumber
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM contracts
                WHERE data ->> 'contractNumber' = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setString(1, contractNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(
                        0L,
                        resultSet.getLong(1),
                        "No debería persistirse un contrato "
                                + "para una obra inexistente."
                );
            }
        }
    }

    private static void deleteContractByNumber(
            Connection connection,
            String contractNumber
    ) throws SQLException {
        String sql = """
                DELETE FROM contracts
                WHERE data ->> 'contractNumber' = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setString(1, contractNumber);
            statement.executeUpdate();
        }
    }

    private static String getDatabaseUrl() {
        String configuredUrl = System.getenv("BRICA_TEST_DB_URL");

        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }

        String databaseName = requiredEnvironmentVariable(
                "POSTGRES_DB"
        );

        return "jdbc:postgresql://localhost:5433/" + databaseName;
    }

    private static String environmentOrDefault(
            String name,
            String defaultValue
    ) {
        String value = System.getenv(name);
        return value == null || value.isBlank()
                ? defaultValue
                : value;
    }

    private static String requiredEnvironmentVariable(
            String... names
    ) {
        for (String name : names) {
            String value = System.getenv(name);

            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        throw new IllegalStateException(
                "Falta una variable de entorno requerida: "
                        + String.join(" o ", names)
        );
    }
}

