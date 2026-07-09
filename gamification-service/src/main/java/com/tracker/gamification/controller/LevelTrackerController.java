package com.tracker.gamification.controller;

import com.tracker.gamification.dto.LevelTrackerDto;
import com.tracker.gamification.dto.LevelTrackerRequestDTO;
import com.tracker.gamification.service.impl.LevelTrackerServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/level")
public class LevelTrackerController {

    @Autowired
    private LevelTrackerServiceImpl levelTrackerService;

    @GetMapping
    public ResponseEntity<List<LevelTrackerDto>> getAllLevelTracker() {
        return ResponseEntity.ok(levelTrackerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LevelTrackerDto> getLevelTrackerById(@PathVariable Long id) {
        return ResponseEntity.ok(levelTrackerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<LevelTrackerDto> createLevelTracker(@RequestBody LevelTrackerRequestDTO levelTrackerRequestDTO) {
        return ResponseEntity.ok(levelTrackerService.save(levelTrackerRequestDTO));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LevelTrackerDto>> getLevelTrackerByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(levelTrackerService.findByUserId(userId));
    }

    @GetMapping("/activity/{activityId}")
    public ResponseEntity<List<LevelTrackerDto>> getLevelTrackerByActivityId(@PathVariable Long activityId) {
        return ResponseEntity.ok(levelTrackerService.findByActivityId(activityId));
    }
}
