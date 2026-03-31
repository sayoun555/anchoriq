package com.anchoriq.api.dto.response.sanction;

import com.anchoriq.core.domain.maritime.sanction.model.Sanction;
import lombok.Builder;
import lombok.Getter;

/**
 * 제재 응답 DTO.
 */
@Getter
@Builder
public class SanctionResponse {

    private final Long id;
    private final String referenceNumber;
    private final String targetName;
    private final String type;
    private final String source;
    private final boolean active;
    private final String startDate;
    private final String endDate;
    private final String description;

    public static SanctionResponse from(Sanction sanction) {
        return SanctionResponse.builder()
                .id(sanction.getId())
                .referenceNumber(sanction.getReferenceNumber())
                .targetName(sanction.getTargetName())
                .type(sanction.getType())
                .source(sanction.getSource())
                .active(sanction.isActive())
                .startDate(sanction.getStartDate() != null ? sanction.getStartDate().toString() : null)
                .endDate(sanction.getEndDate() != null ? sanction.getEndDate().toString() : null)
                .description(sanction.getDescription())
                .build();
    }
}
