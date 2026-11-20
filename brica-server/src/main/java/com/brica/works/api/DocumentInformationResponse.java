package com.brica.works.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import com.brica.works.domain.Work;
import com.brica.works.domain.WorkStatus;

public record DocumentInformationResponse(String contractNumber, String contractor, BigDecimal contractedAmount,
        BigDecimal amountBeforeTax, BigDecimal taxAmount, LocalDate startDate, LocalDate endDate,
        LocalDate actualEndDate, WorkStatus status,
        Map<String, String> additionalValues) {
    public static DocumentInformationResponse from(Work work) {
        return new DocumentInformationResponse(work.getContractNumber(), work.getContractor(), work.getContractedAmount(),
                work.getAmountBeforeTax(), work.getTaxAmount(), work.getStartDate(), work.getEndDate(),
                work.getActualEndDate(), work.getStatus(), work.getAdditionalValues());
    }
}
