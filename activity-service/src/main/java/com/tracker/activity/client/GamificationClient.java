package com.tracker.activity.client;


import com.tracker.activity.dto.LevelTrackerDto;
import com.tracker.activity.dto.LevelTrackerRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gamification-service")
public interface GamificationClient {

//    @GetMapping("/level")
//    List<LevelTrackerDto> getAllLevelTracker();
//
//    @GetMapping("/level/{id}")
//    LevelTrackerDto getLevelTrackerById(@PathVariable("id") Long id);

    @PostMapping("/level")
    LevelTrackerDto createLevelTracker(@RequestHeader("userId") Long userId, @RequestBody LevelTrackerRequestDTO request);

//    @GetMapping("/level/user/{userId}")
//    List<LevelTrackerDto> getLevelTrackerByUserId(
//            @PathVariable("userId") Long userId
//    );
//
//    @GetMapping("/level/activity/{activityId}")
//    List<LevelTrackerDto> getLevelTrackerByActivityId(
//            @PathVariable("activityId") Long activityId
//    );
}
