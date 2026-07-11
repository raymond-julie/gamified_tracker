package com.tracker.activity.service.impl;

import com.tracker.activity.client.GamificationClient;
import com.tracker.activity.dao.ActivityLog;
import com.tracker.activity.dto.ActivityLogResponse;
import com.tracker.activity.dto.ActivityLogRequest;
import com.tracker.activity.dto.LevelTrackerRequestDTO;
import com.tracker.activity.exception.ActivityNotFoundException;
import com.tracker.activity.repository.ActivityLogRepository;
import com.tracker.activity.repository.ActivityRepository;
import com.tracker.activity.service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.random.RandomGenerator;


@Service
public class ActivityLogServiceImpl implements ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final ActivityRepository activityRepository;
    private final GamificationClient gamificationClient;

    public ActivityLogServiceImpl(ActivityLogRepository activityLogRepository, ActivityRepository activityRepository, GamificationClient gamificationClient) {
        this.activityLogRepository = activityLogRepository;
        this.activityRepository = activityRepository;
        this.gamificationClient = gamificationClient;
    }

    @Override
    public ResponseEntity<ActivityLogResponse> getActivityLogResponseEntity(Long id) {
        var activityLog = activityLogRepository.findById(id)
                .orElseThrow(() -> new ActivityNotFoundException("Activity log not found: " + id));

        return ResponseEntity.ok(mapToActivityLogResponse(activityLog));
    }

    @Override
    public ResponseEntity<ActivityLogResponse> addActivityLogResponseResponseEntity(Long userId, ActivityLogRequest activityLogRequest) {
        var activityLog = mapToActivityLog(userId, activityLogRequest);
        activityLog.setDurationMinutes(Duration.between(activityLog.getStartTime(), activityLog.getEndTime()).toMinutes());
        activityLog.setUserId(userId);

        var random = RandomGenerator.getDefault();
        double multiplier = activityLog.getActivity().getXpMultiplier();
        double bonus = random.nextDouble() < 0.2 ? random.nextDouble(1.1, 1.5) : 1.0; // 20% chance of a bonus roll
        activityLog.setXpEarned(activityLog.getDurationMinutes() * multiplier * bonus);

        gamificationClient.createLevelTracker(userId, new LevelTrackerRequestDTO(activityLog.getActivity().getId(), activityLog.getXpEarned()));

        activityLogRepository.save(activityLog);

        return ResponseEntity.ok(mapToActivityLogResponse(activityLog));
    }

    public ResponseEntity<List<ActivityLogResponse>> getAllActivityForUser(Long id) {
        var activityLogList = activityLogRepository.findByUserId(id);

        var activityLogResponses = activityLogList.stream().map(this::mapToActivityLogResponse).toList();

        return ResponseEntity.ok(activityLogResponses);
    }

    private ActivityLog mapToActivityLog(Long userId, ActivityLogRequest activityLogRequest) {
        return ActivityLog.builder()
                .userId(userId)
                .activity(activityRepository.findByName(activityLogRequest.activityName())
                        .orElseThrow(() -> new ActivityNotFoundException("Activity not found: " + activityLogRequest.activityName())))
                .startTime(activityLogRequest.startTime())
                .endTime(activityLogRequest.endTime())
                .notes(activityLogRequest.notes())
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
