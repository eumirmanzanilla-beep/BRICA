package com.brica.contracts.api;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterContractRequest(

        @NotNull(message = "El campo es obligatorio.")
        @Positive(message = "El identificador debe ser positivo.")
        Long workId,

        Map<String, Object> data

) {
}
