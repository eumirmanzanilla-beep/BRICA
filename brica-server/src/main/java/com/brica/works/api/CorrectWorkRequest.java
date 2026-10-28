package com.brica.works.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Complete replacement of general information, never contractual conditions. */
public record CorrectWorkRequest(
        @NotBlank(message = "El campo es obligatorio.")
        @Size(max = 255, message = "El campo no debe superar 255 caracteres.") String name,
        @NotBlank(message = "El campo es obligatorio.")
        @Size(max = 255, message = "El campo no debe superar 255 caracteres.") String location,
        String description) { }
