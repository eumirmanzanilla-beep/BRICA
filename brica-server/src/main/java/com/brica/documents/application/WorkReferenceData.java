package com.brica.documents.application;

import com.brica.documents.domain.ReferenceAttribute;

public record WorkReferenceData(String workName, String clientName) {
    public String valueOf(ReferenceAttribute attribute) {
        return switch (attribute) {
            case WORK_NAME -> workName;
            case CLIENT_NAME -> clientName;
        };
    }
}
