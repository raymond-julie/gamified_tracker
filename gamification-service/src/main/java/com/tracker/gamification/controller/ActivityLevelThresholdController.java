package com.tracker.gamification.controller;

import com.tracker.gamification.dto.ActivityLevelThresholdDto;
import com.tracker.gamification.service.impl.ActivityLevelThresholdServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/threshold")
public class ActivityLevelThresholdController {
    @Autowired
    private ActivityLevelThresholdServiceImpl activityLevelThresholdService;

    @GetMapping
    public ResponseEntity<List<ActivityLevelThresholdDto>> getActivityLevelThreshold() {
        return ResponseEntity.ok(activityLevelThresholdService.getAllActivityLevelThreshold());
    }

    @PostMapping("/activity")
    public ResponseEntity<ActivityLevelThresholdDto> getActivityLevelThresholdById(@RequestBody ActivityLevelThresholdDto activityLevelThresholdDto) {
        return ResponseEntity.ok(activityLevelThresholdService.getActivityLevelThresholdById(activityLevelThresholdDto));
    }

    @PostMapping
    public ResponseEntity<ActivityLevelThresholdDto> createActivityLevelThreshold(@RequestBody ActivityLevelThresholdDto activityLevelThresholdDto) {
        return ResponseEntity.ok(activityLevelThresholdService.saveActivityLevelThreshold(activityLevelThresholdDto));
    }
}
