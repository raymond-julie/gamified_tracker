package com.tracker.gamification.repository;

import com.tracker.gamification.dao.ActivityLevelThreshold;
import com.tracker.gamification.dao.ActivityLevelThresholdId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLevelThresholdRepository extends JpaRepository<ActivityLevelThreshold, ActivityLevelThresholdId> {

    @Query("""
            SELECT a
            FROM ActivityLevelThreshold a
            WHERE a.id.activityId = :activityId
            AND a.xpRequired <= :xp
            ORDER BY a.id.level DESC
            """)
    List<ActivityLevelThreshold> findReachedLevels(
            @Param("activityId") Long activityId,
            @Param("xp") double xp,
            Pageable pageable
    );
}
