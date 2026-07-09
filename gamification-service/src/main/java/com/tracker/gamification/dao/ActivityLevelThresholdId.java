package com.tracker.gamification.dao;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
@Embeddable
public class ActivityLevelThresholdId implements Serializable {

    private Long activityId;

    private Integer level;


}
