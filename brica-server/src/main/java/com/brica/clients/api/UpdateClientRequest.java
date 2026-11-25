
package com.brica.clients.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateClientRequest(

        @NotBlank(message = "El campo es obligatorio.")
        @Size(
                max = 255,
                message = "El campo no debe superar 255 caracteres."
        )
        @Pattern(
                regexp = "^[\\p{L}][\\p{L}\\p{N} _-]*$",
                message = "El nombre del cliente debe iniciar con una letra y solo admite letras, números, espacios y guiones."
        )
        String name,

        @NotBlank(message = "El campo es obligatorio.")
        @Size(
                max = 255,
                message = "El campo no debe superar 255 caracteres."
        )
        String legalRepresentative

) {
}
