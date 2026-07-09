package com.tracker.gamification.repository;

import com.tracker.gamification.dao.ActivityLevelThreshold;
import com.tracker.gamification.dao.ActivityLevelThresholdId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ActivityLevelThresholdRepositoryTest {

    @Autowired
    private ActivityLevelThresholdRepository activityLevelThresholdRepository;

    @Test
    void testFindReachedLevels() {

        // Arrange
        Long activityId = 1L;

        activityLevelThresholdRepository.saveAll(List.of(
                createThreshold(activityId, 1, 0),
                createThreshold(activityId, 2, 100),
                createThreshold(activityId, 3, 300)
        ));

        // Act
        List<ActivityLevelThreshold> reachedLevels =
                activityLevelThresholdRepository.findReachedLevels(
                        activityId,
                        250.0,
                        PageRequest.of(0, 10)
                );

        // Assert
        assertEquals(2, reachedLevels.size());
        assertEquals(2, reachedLevels.get(0).getId().getLevel());
        assertEquals(1, reachedLevels.get(1).getId().getLevel());
    }

    @Test
    void testFindReachedLevelsNoResults() {

        // Arrange
        activityLevelThresholdRepository.save(
                createThreshold(2L, 1, 500)
        );

        // Act
        List<ActivityLevelThreshold> reachedLevels =
                activityLevelThresholdRepository.findReachedLevels(
                        2L,
                        100.0,
                        PageRequest.of(0, 10)
                );

        // Assert
        assertTrue(reachedLevels.isEmpty());
    }

    @Test
    void testFindReachedLevelsWithPagination() {

        Long activityId = 1L;

        activityLevelThresholdRepository.saveAll(List.of(
                createThreshold(activityId, 1, 0),
                createThreshold(activityId, 2, 100),
                createThreshold(activityId, 3, 300)
        ));

        var reachedLevels = activityLevelThresholdRepository.findReachedLevels(
                activityId,
                250.0,
                PageRequest.of(0, 1)
        );

        assertEquals(1, reachedLevels.size());
        assertEquals(2, reachedLevels.get(0).getId().getLevel());
    }

    private ActivityLevelThreshold createThreshold(
            Long activityId,
            int level,
            double xpRequired) {

        return ActivityLevelThreshold.builder()
                .id(ActivityLevelThresholdId.builder()
                        .activityId(activityId)
                        .level(level)
                        .build())
                .xpRequired(xpRequired)
                .build();
    }
}