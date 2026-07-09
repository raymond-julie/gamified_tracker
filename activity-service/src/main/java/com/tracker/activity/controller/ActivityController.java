package com.tracker.activity.controller;

import com.tracker.activity.dto.ActivityResponseRecord;
import com.tracker.activity.dto.ActivityRequestRecord;
import com.tracker.activity.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/{name}")
    public ResponseEntity<ActivityResponseRecord> getActivity(@PathVariable String name) {
        return activityService.getActivity(name);
    }

    @GetMapping("/")
    public ResponseEntity<List<ActivityResponseRecord>> getAllActivities() {
        return activityService.getAllActivities();
    }

    @PostMapping("/")
    public ResponseEntity<ActivityResponseRecord> addActivity(@RequestBody ActivityRequestRecord activityRequest) {
        return activityService.addActivityEntity(activityRequest);
    }
}
