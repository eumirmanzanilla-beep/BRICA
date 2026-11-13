package com.brica.documents.application;

import java.util.Map;
import com.brica.documents.domain.ReferenceAttribute;
import com.brica.documents.domain.ReferenceField;
import com.brica.documents.domain.ReferenceOrigin;
import com.brica.works.domain.Work;

public class WorkReferenceData {
    private final String workName;
    private final String clientName;
    private final Work work;
    public WorkReferenceData(String workName, String clientName) {
        this.workName = workName; this.clientName = clientName; this.work = null;
    }
    public WorkReferenceData(Work work) {
        this.work = work; this.workName = work.getName(); this.clientName = work.getClient().getName();
    }
    public Object valueOf(ReferenceField field) {
        if (field.getOrigin() == ReferenceOrigin.ADDITIONAL) {
            return work == null ? null : work.getAdditionalValues().get(field.getSourcePath());
        }
        return valueOf(field.getAttribute());
    }
    public Object valueOf(ReferenceAttribute attribute) {
        if (attribute == ReferenceAttribute.WORK_NAME) { return workName; }
        if (attribute == ReferenceAttribute.CLIENT_NAME) { return clientName; }
        if (work == null) { return null; }
        return switch (attribute) {
            case WORK_NAME -> workName;
            case CLIENT_NAME -> clientName;
            case WORK_ID -> String.valueOf(work.getId());
            case WORK_CLIENT_ID, CLIENT_ID -> String.valueOf(work.getClient().getId());
            case CLIENT_LEGAL_REPRESENTATIVE -> work.getClient().getLegalRepresentative();
            case WORK_CONTRACT_NUMBER -> work.getContractNumber();
            case WORK_CONTRACTOR -> work.getContractor();
            case WORK_LOCATION -> work.getLocation();
            case WORK_DESCRIPTION -> work.getDescription();
            case WORK_STATUS -> work.getStatus().label();
            case WORK_START_DATE -> work.getStartDate();
            case WORK_END_DATE -> work.getEndDate();
            case WORK_ACTUAL_END_DATE -> work.getActualEndDate();
            case WORK_CONTRACTED_AMOUNT -> work.getContractedAmount();
            case WORK_AMOUNT_BEFORE_TAX -> work.getAmountBeforeTax();
            case WORK_TAX_AMOUNT -> work.getTaxAmount();
        };
    }
}
