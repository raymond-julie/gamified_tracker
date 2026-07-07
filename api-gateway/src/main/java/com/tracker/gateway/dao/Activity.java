package com.tracker.gateway.dao;

import java.time.LocalDateTime;

public record Activity(
        Long id,
        String name,
        Category category,
        double xpMultiplier,
        boolean active,
        String description,
        LocalDateTime createdAt
) {
}
