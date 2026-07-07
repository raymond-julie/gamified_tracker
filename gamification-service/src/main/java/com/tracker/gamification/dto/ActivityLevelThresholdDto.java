package com.tracker.gamification.dto;

public record ActivityLevelThresholdDto(
        Long activityId,
        Integer level,
        double xpRequired
) {
}
