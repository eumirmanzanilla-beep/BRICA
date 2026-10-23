package com.brica.contracts.api;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record RegisterContractRequest(

        @NotNull(message = "workId is required")
        Long workId,

        Map<String, Object> data

) {
}