package com.tracker.gamification.dto;

import java.time.LocalDateTime;

public record LevelUpEventDto(Long id,
                              Long activityId,
                              Integer oldLevel,
                              Integer newLevel,
                              double totalXp,
                              double currentLevelXp,
                              boolean read,
                              LocalDateTime createdAt
) {
}
