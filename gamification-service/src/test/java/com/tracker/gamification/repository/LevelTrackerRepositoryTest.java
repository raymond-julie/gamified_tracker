package com.tracker.gamification.repository;

import com.tracker.gamification.dao.LevelTracker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class LevelTrackerRepositoryTest {

    @Autowired
    private LevelTrackerRepository levelTrackerRepository;

    @Test
    void testFindByUserIdAndActivityId() {
        // Arrange
        Long userId = 1L;
        Long activityId = 1L;

        LevelTracker tracker = LevelTracker.builder()
                .userId(userId)
                .activityId(activityId)
                .level(5)
                .totalXp(500.0)
                .currentLevelXp(250.0)
                .build();

        LevelTracker saved = levelTrackerRepository.save(tracker);

        // Act
        Optional<LevelTracker> result = levelTrackerRepository.findByUserIdAndActivityId(userId, activityId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
        assertEquals(userId, result.get().getUserId());
        assertEquals(activityId, result.get().getActivityId());
        assertEquals(5, result.get().getLevel());
    }

    @Test
    void testFindByUserIdAndActivityIdNotFound() {
        // Arrange
        Long userId = 99L;
        Long activityId = 99L;

        // Act
        Optional<LevelTracker> result = levelTrackerRepository.findByUserIdAndActivityId(userId, activityId);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAllByUserId() {
        // Arrange
        Long userId = 2L;

        LevelTracker tracker1 = LevelTracker.builder()
                .userId(userId)
                .activityId(1L)
                .level(3)
                .totalXp(300.0)
                .currentLevelXp(150.0)
                .build();

        LevelTracker tracker2 = LevelTracker.builder()
                .userId(userId)
                .activityId(2L)
                .level(2)
                .totalXp(200.0)
                .currentLevelXp(100.0)
                .build();

        LevelTracker tracker3 = LevelTracker.builder()
                .userId(userId)
                .activityId(3L)
                .level(4)
                .totalXp(400.0)
                .currentLevelXp(200.0)
                .build();

        levelTrackerRepository.save(tracker1);
        levelTrackerRepository.save(tracker2);
        levelTrackerRepository.save(tracker3);

        // Act
        List<LevelTracker> results = levelTrackerRepository.findAllByUserId(userId);

        // Assert
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(t -> t.getUserId().equals(userId)));
    }

    @Test
    void testFindAllByUserIdNoResults() {
        // Arrange
        Long userId = 99L;

        // Act
        List<LevelTracker> results = levelTrackerRepository.findAllByUserId(userId);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindAllByActivityId() {
        // Arrange
        Long activityId = 1L;

        LevelTracker tracker1 = LevelTracker.builder()
                .userId(1L)
                .activityId(activityId)
                .level(3)
                .totalXp(300.0)
                .currentLevelXp(150.0)
                .build();

        LevelTracker tracker2 = LevelTracker.builder()
                .userId(2L)
                .activityId(activityId)
                .level(2)
                .totalXp(200.0)
                .currentLevelXp(100.0)
                .build();

        LevelTracker tracker3 = LevelTracker.builder()
                .userId(3L)
                .activityId(activityId)
                .level(5)
                .totalXp(500.0)
                .currentLevelXp(250.0)
                .build();

        levelTrackerRepository.save(tracker1);
        levelTrackerRepository.save(tracker2);
        levelTrackerRepository.save(tracker3);

        // Act
        List<LevelTracker> results = levelTrackerRepository.findAllByActivityId(activityId);

        // Assert
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(t -> t.getActivityId().equals(activityId)));
    }

    @Test
    void testFindAllByActivityIdNoResults() {
        // Arrange
        Long activityId = 99L;

        // Act
        List<LevelTracker> results = levelTrackerRepository.findAllByActivityId(activityId);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetTotalXpByUserId() {
        // Arrange
        Long userId = 3L;

        LevelTracker tracker1 = LevelTracker.builder()
                .userId(userId)
                .activityId(1L)
                .level(2)
                .totalXp(250.0)
                .currentLevelXp(100.0)
                .build();

        LevelTracker tracker2 = LevelTracker.builder()
                .userId(userId)
                .activityId(2L)
                .level(3)
                .totalXp(300.0)
                .currentLevelXp(150.0)
                .build();

        LevelTracker tracker3 = LevelTracker.builder()
                .userId(userId)
                .activityId(3L)
                .level(1)
                .totalXp(100.0)
                .currentLevelXp(50.0)
                .build();

        levelTrackerRepository.save(tracker1);
        levelTrackerRepository.save(tracker2);
        levelTrackerRepository.save(tracker3);

        // Act
        Double totalXp = levelTrackerRepository.getTotalXpByUserId(userId);

        // Assert
        assertNotNull(totalXp);
        assertEquals(650.0, totalXp);
    }

    @Test
    void testGetTotalXpByUserIdNoRecords() {
        // Arrange
        Long userId = 99L;

        // Act
        Double totalXp = levelTrackerRepository.getTotalXpByUserId(userId);

        // Assert
        assertNotNull(totalXp);
        assertEquals(0.0, totalXp);
    }
}

