package com.tracker.gateway.dto;

import java.time.LocalDateTime;

public record AddActivityLogRequest(
        Long userId,
        String activityName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String notes,
        LocalDateTime createdAt
) {
}
