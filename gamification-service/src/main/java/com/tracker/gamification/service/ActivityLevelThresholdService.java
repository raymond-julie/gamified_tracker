package com.tracker.gamification.service;

import com.tracker.gamification.dto.ActivityLevelThresholdDto;

import java.util.List;

public interface ActivityLevelThresholdService {

    ActivityLevelThresholdDto getActivityLevelThresholdById(
            ActivityLevelThresholdDto activityLevelThresholdDto);

    ActivityLevelThresholdDto saveActivityLevelThreshold(
            ActivityLevelThresholdDto activityLevelThresholdDto);

    List<ActivityLevelThresholdDto> getAllActivityLevelThreshold();
}
