package com.tracker.activity.service;

import com.tracker.activity.client.GamificationClient;
import com.tracker.activity.dao.ActivityLog;
import com.tracker.activity.dto.ActivityLogResponse;
import com.tracker.activity.dto.AddActivityLogRequest;
import com.tracker.activity.dto.LevelTrackerRequestDTO;
import com.tracker.activity.exception.ActivityNotFoundException;
import com.tracker.activity.repository.ActivityLogRepository;
import com.tracker.activity.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.random.RandomGenerator;


@Service
public class ActivityLogService {
    @Autowired
    ActivityLogRepository activityLogRepository;

    @Autowired
    ActivityRepository activityRepository;

    @Autowired
    GamificationClient gamificationClient;

    public ResponseEntity<ActivityLogResponse> getActivityLogResponseEntity(Long id) {
        var activityLog = activityLogRepository.findById(id)
                .orElseThrow(() -> new ActivityNotFoundException("Activity log not found: " + id));

        return ResponseEntity.ok(mapToActivityLogResponse(activityLog));
    }

    public ResponseEntity<ActivityLogResponse> addActivityLogResponseResponseEntity(AddActivityLogRequest addActivityLogRequest) {
        var activityLog = mapToActivityLog(addActivityLogRequest);
        activityLog.setDurationMinutes(Duration.between(activityLog.getStartTime(), activityLog.getEndTime()).toMinutes());

        var random = RandomGenerator.getDefault();
        double multiplier = activityLog.getActivity().getXpMultiplier();
        double bonus = random.nextDouble() < 0.2 ? random.nextDouble(1.1, 1.5) : 1.0; // 20% chance of a bonus roll
        activityLog.setXpEarned(activityLog.getDurationMinutes() * multiplier * bonus);

        gamificationClient.createLevelTracker(new LevelTrackerRequestDTO(activityLog.getUserId(), activityLog.getActivity().getId(), activityLog.getXpEarned()));

        activityLogRepository.save(activityLog);

        return ResponseEntity.ok(mapToActivityLogResponse(activityLog));
    }

    public ResponseEntity<List<ActivityLogResponse>> getAllActivityForUser(Long id) {
        var activityLogList = activityLogRepository.findByUserId(id);

        var activityLogResponses = activityLogList.stream().map(this::mapToActivityLogResponse).toList();

        return ResponseEntity.ok(activityLogResponses);
    }

    private ActivityLog mapToActivityLog(AddActivityLogRequest addActivityLogRequest) {
        return ActivityLog.builder()
                .userId(addActivityLogRequest.userId())
                .activity(activityRepository.findByName(addActivityLogRequest.activityName())
                        .orElseThrow(() -> new ActivityNotFoundException("Activity not found: " + addActivityLogRequest.activityName())))
                .startTime(addActivityLogRequest.startTime())
                .endTime(addActivityLogRequest.endTime())
                .notes(addActivityLogRequest.notes())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ActivityLogResponse mapToActivityLogResponse(ActivityLog activityLog) {
        return new ActivityLogResponse(
                activityLog.getId(),
                activityLog.getUserId(),
                activityLog.getActivity(),
                activityLog.getStartTime(),
                activityLog.getEndTime(),
                activityLog.getDurationMinutes(),
                activityLog.getXpEarned(),
                activityLog.getNotes(),
                activityLog.getCreatedAt()
        );
    }
}
