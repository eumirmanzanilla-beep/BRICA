package com.brica.works.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterWorkRequest(

        @NotNull(message = "El campo es obligatorio.")
        @Positive(message = "El identificador debe ser positivo.")
        Long clientId,

        @NotBlank(message = "El campo es obligatorio.")
        @Size(
            max = 255,
            message = "El campo no debe superar 255 caracteres."
        )
        @Pattern(
            regexp = "^[\\p{L}][\\p{L}\\p{N} _-]*$",
            message = "El nombre de la obra debe iniciar con una letra y solo admite letras, números, espacios y guiones."
        )
        String name,

        @Size(
            max = 255,
            message = "El campo no debe superar 255 caracteres."
        )
        String location,

        String description,

        LocalDate startDate,

        LocalDate endDate

) {
}
