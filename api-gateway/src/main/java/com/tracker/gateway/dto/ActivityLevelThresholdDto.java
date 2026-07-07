package com.tracker.gateway.dto;

public record ActivityLevelThresholdDto(
        Long activityId,
        Integer level,
        double xpRequired
) {
}
