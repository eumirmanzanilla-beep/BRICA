
package com.brica.clients.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateClientRequest(

        @NotBlank(message = "El campo es obligatorio.")
        @Size(
                max = 255,
                message = "El campo no debe superar 255 caracteres."
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
