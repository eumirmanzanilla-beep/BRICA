package com.brica.works.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterWorkRequest(

        @NotNull(message = "clientId is required")
        Long clientId,

        @NotBlank(message = "name must not be blank")
        @Size(
            max = 255,
            message = "name must not exceed 255 characters"
        )
        String name,

        @NotBlank(message = "location must not be blank")
        @Size(
            max = 255,
            message = "location must not exceed 255 characters"
        )
        String location,

        String description,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        LocalDate endDate

) {
}