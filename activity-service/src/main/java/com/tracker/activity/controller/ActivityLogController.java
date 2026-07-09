package com.tracker.activity.controller;

import com.tracker.activity.dto.ActivityLogResponse;
import com.tracker.activity.dto.ActivityLogRequest;
import com.tracker.activity.service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/activitylog")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityLogResponse> getActivityLog(@PathVariable("id") Long id) {
        return activityLogService.getActivityLogResponseEntity(id);
    }

    @PostMapping("/")
    public ResponseEntity<ActivityLogResponse> addActivityLog(@RequestBody ActivityLogRequest activityLogRequest) {
        return activityLogService.addActivityLogResponseResponseEntity(activityLogRequest);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<ActivityLogResponse>> getAllActivityForUser(@PathVariable("id") Long id) {
        return activityLogService.getAllActivityForUser(id);
    }
}
