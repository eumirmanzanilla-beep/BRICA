package com.brica.works.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Complete replacement of general information, never contractual conditions. */
public record CorrectWorkRequest(
        @NotBlank(message = "El campo es obligatorio.")
        @Size(max = 255, message = "El campo no debe superar 255 caracteres.")
        @Pattern(regexp = "^[\\p{L}][\\p{L}\\p{N} _-]*$",
                message = "El nombre de la obra debe iniciar con una letra y solo admite letras, números, espacios y guiones.") String name,
        @Size(max = 255, message = "El campo no debe superar 255 caracteres.") String location,
        String description) { }
