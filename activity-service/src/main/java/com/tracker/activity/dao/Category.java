package com.tracker.activity.dao;

public enum Category {
    STUDY,
    WORK,
    GAMING,
    CHORES,
    HEALTH,
    OTHER;

    public double baseXpMultiplier() {
        return switch (this) {
            case STUDY, WORK -> 1.5;
            case HEALTH -> 1.3;
            case OTHER -> 1.0;
            case CHORES -> 0.8;
            case GAMING -> 0.5;
        };
    }
}
