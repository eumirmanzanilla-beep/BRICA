package com.brica.client.works.api;

public record WorkDto(
        Long id,
        Long clientId,
        String name,
        String location,
        String description,
        String startDate,
        String endDate,
        String documentName
) {
    public WorkDto(Long id, Long clientId, String name, String location, String description, String startDate, String endDate) {
        this(id, clientId, name, location, description, startDate, endDate, null);
    }
}
