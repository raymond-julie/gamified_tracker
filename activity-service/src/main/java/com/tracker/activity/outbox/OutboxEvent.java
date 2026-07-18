private static final String EVENT_TYPE_ACTIVITY_LOGGED = "ActivityLogged";private static final String ACTIVITY_LOG_TYPE = "ActivityLog";package com.tracker.activity.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;   // ActivityLogType.ACTIVITY_LOG
    private Long aggregateId;
    private String eventType;       // EventType.ACTIVITY_LOGGED

    @Column(columnDefinition = "text")
    private String payload;         // JSON of ActivityLoggedEvent

    @Column(unique = true)
    private String idempotencyKey;

    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;  // null until the relay publishes it
}