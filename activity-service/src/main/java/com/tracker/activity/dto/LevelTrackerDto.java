package com.tracker.activity.dto;

public record LevelTrackerDto(
        Long userId,
        Long activityId,
        Integer level,
        Integer totalXp,
        Integer currentLevelXp
) {
}
