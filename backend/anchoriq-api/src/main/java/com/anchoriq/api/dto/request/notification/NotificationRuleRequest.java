package com.anchoriq.api.dto.request.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 규칙 생성/수정 요청 DTO.
 */
@Getter
@NoArgsConstructor
public class NotificationRuleRequest {

    @NotBlank(message = "Rule name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Condition type is required")
    private String conditionType;

    @NotBlank(message = "Condition value is required")
    private String conditionValue;

    @NotBlank(message = "Channel is required")
    private String channel;
}
