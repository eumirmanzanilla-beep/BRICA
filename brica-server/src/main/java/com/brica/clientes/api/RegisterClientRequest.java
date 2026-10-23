package com.brica.clientes.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterClientRequest(

        @NotBlank(message = "name must not be blank")
        @Size(
            max = 255,
            message = "name must not exceed 255 characters"
        )
        String name,

        @NotBlank(
            message = "legalRepresentative must not be blank"
        )
        @Size(
            max = 255,
            message = "legalRepresentative must not exceed 255 characters"
        )
        String legalRepresentative

) {
}