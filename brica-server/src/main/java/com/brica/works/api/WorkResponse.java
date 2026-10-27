package com.brica.works.api;

import java.time.LocalDate;

import com.brica.works.domain.Work;

public record WorkResponse(
        Long id,
        Long clientId,
        String name,
        String location,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {

    public static WorkResponse from(Work work) {
        return new WorkResponse(
            work.getId(),
            work.getClient().getId(),
            work.getName(),
            work.getLocation(),
            work.getDescription(),
            work.getStartDate(),
            work.getEndDate()
        );
    }
}