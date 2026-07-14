package com.tracker.gamification.repository;

import com.tracker.gamification.dao.LevelUpEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LevelUpEventRepository extends JpaRepository<LevelUpEvent, Long> {
    List<LevelUpEvent> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<LevelUpEvent> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    Optional<LevelUpEvent> findByIdAndUserId(Long id, Long userId);
}
