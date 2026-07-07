package com.tracker.gateway.dto;

import com.tracker.gateway.dao.Category;

import java.time.LocalDateTime;

public record ActivityResponse(
        String name,
        Category category,
        double xpMultiplier,
        boolean active,
        String description,
        LocalDateTime createdAt
) {
}
