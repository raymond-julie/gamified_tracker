package com.tracker.gamification.service;

import com.tracker.gamification.dto.LevelUpEventDto;

import java.util.List;

public interface NotificationService {

    List<LevelUpEventDto> getForUser(Long userId, boolean unreadOnly);

    long unreadCount(Long userId);

    void markRead(Long userId, Long eventId);
}
