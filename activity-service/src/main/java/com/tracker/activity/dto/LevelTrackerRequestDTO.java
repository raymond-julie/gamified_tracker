package com.tracker.activity.dto;

public record LevelTrackerRequestDTO(Long activityId, double xp) {
    public LevelTrackerRequestDTO {
        if (xp < 0) throw new IllegalArgumentException("xp cannot be negative");
    }
}
