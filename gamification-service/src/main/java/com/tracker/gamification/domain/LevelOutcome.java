package com.tracker.gamification.domain;

public sealed interface LevelOutcome permits LevelOutcome.LeveledUp, LevelOutcome.InProgress {
    record LeveledUp(int level, double currentLevelXp) implements LevelOutcome {}
    record InProgress(int level, double currentLevelXp) implements LevelOutcome {}
}
