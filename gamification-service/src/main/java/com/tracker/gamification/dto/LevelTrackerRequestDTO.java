package com.tracker.gamification.dto;

public record LevelTrackerRequestDTO(Long userId, Long activityId, double xp) {
    public LevelTrackerRequestDTO {
        if (xp < 0) throw new IllegalArgumentException("xp cannot be negative");
    }
}
