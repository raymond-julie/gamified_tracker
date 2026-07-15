package com.tracker.gamification.repository;

import com.tracker.gamification.dao.LevelUpEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for the LevelUpEvent.read <-> is_read column mapping: the derived
 * query method names below (…AndReadFalse…) only resolve correctly if the JPA attribute
 * is named "read", not "isRead" - this exact mismatch previously broke context startup.
 */
@DataJpaTest
class LevelUpEventRepositoryTest {

    @Autowired
    private LevelUpEventRepository levelUpEventRepository;

    private LevelUpEvent event(Long userId, Long activityId, boolean read, LocalDateTime createdAt) {
        return LevelUpEvent.builder()
                .userId(userId)
                .activityId(activityId)
                .oldLevel(1)
                .newLevel(2)
                .totalXp(300.0)
                .currentLevelXp(100.0)
                .read(read)
                .createdAt(createdAt)
                .build();
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsOnlyThatUsersEventsNewestFirst() {
        LocalDateTime now = LocalDateTime.now();
        levelUpEventRepository.save(event(1L, 1L, true, now.minusMinutes(10)));
        levelUpEventRepository.save(event(1L, 2L, false, now));
        levelUpEventRepository.save(event(2L, 1L, false, now));

        List<LevelUpEvent> results = levelUpEventRepository.findByUserIdOrderByCreatedAtDesc(1L);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(e -> e.getUserId().equals(1L)));
        assertEquals(2L, results.get(0).getActivityId());
        assertEquals(1L, results.get(1).getActivityId());
    }

    @Test
    void findByUserIdAndReadFalseOrderByCreatedAtDesc_excludesReadEvents() {
        LocalDateTime now = LocalDateTime.now();
        levelUpEventRepository.save(event(3L, 1L, true, now.minusMinutes(5)));
        levelUpEventRepository.save(event(3L, 2L, false, now));

        List<LevelUpEvent> results = levelUpEventRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(3L);

        assertEquals(1, results.size());
        assertFalse(results.get(0).isRead());
        assertEquals(2L, results.get(0).getActivityId());
    }

    @Test
    void countByUserIdAndReadFalse_countsOnlyUnread() {
        levelUpEventRepository.save(event(4L, 1L, true, LocalDateTime.now()));
        levelUpEventRepository.save(event(4L, 2L, false, LocalDateTime.now()));
        levelUpEventRepository.save(event(4L, 3L, false, LocalDateTime.now()));

        assertEquals(2L, levelUpEventRepository.countByUserIdAndReadFalse(4L));
    }

    @Test
    void findByIdAndUserId_presentWhenOwnedByCaller() {
        LevelUpEvent saved = levelUpEventRepository.save(event(5L, 1L, false, LocalDateTime.now()));

        Optional<LevelUpEvent> result = levelUpEventRepository.findByIdAndUserId(saved.getId(), 5L);

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }

    @Test
    void findByIdAndUserId_emptyWhenOwnedByDifferentUser() {
        LevelUpEvent saved = levelUpEventRepository.save(event(6L, 1L, false, LocalDateTime.now()));

        Optional<LevelUpEvent> result = levelUpEventRepository.findByIdAndUserId(saved.getId(), 999L);

        assertTrue(result.isEmpty());
    }
}
