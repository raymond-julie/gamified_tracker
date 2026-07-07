package com.tracker.activity.dto;

import com.tracker.activity.dao.Category;

import java.time.LocalDateTime;

public record AddActivityRequest(
        String name,
        Category category,
        double xpMultiplier,
        boolean active,
        String description,
        LocalDateTime createdAt
) {
}
