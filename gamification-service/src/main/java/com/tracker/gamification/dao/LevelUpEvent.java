package com.tracker.gamification.dao;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "level_up_event")
public class LevelUpEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long activityId;

    private Integer oldLevel;
    private Integer newLevel;

    private double totalXp;
    private double currentLevelXp;

    // Field is named `read` (not `isRead`) so the JPA attribute resolves to `read`,
    // matching LevelUpEventRepository's derived queries (…AndReadFalse…). Lombok still
    // generates isRead()/setRead(); the DB column stays `is_read`.
    @Column(name = "is_read")
    @Builder.Default
    private boolean read = false;

    private LocalDateTime createdAt;
}
