package com.brica.client.api;

public record WorkDto(
        Long id,
        Long clientId,
        String name,
        String location,
        String description,
        String startDate,
        String endDate
) {
}
