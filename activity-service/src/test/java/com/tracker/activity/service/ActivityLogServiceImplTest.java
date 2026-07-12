package com.tracker.activity.service;

import com.tracker.activity.client.GamificationClient;
import com.tracker.activity.dao.Activity;
import com.tracker.activity.dao.ActivityLog;
import com.tracker.activity.dao.Category;
import com.tracker.activity.dto.ActivityLogRequest;
import com.tracker.activity.dto.ActivityLogResponse;
import com.tracker.activity.dto.LevelTrackerRequestDTO;
import com.tracker.activity.exception.ActivityNotFoundException;
import com.tracker.activity.repository.ActivityLogRepository;
import com.tracker.activity.repository.ActivityRepository;
import com.tracker.activity.service.impl.ActivityLogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityLog Service Tests")
public class ActivityLogServiceImplTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private GamificationClient gamificationClient;

    @InjectMocks
    private ActivityLogServiceImpl activityLogService;

    @Test
    @DisplayName("getActivityLogResponseEntity returns mapped response")
    void testGetActivityLogResponseEntity() {
        LocalDateTime now = LocalDateTime.now();
        Activity activity = Activity.builder().id(5L).name("Run").category(Category.HEALTH).xpMultiplier(1.2).active(true).createdAt(now).build();
        ActivityLog log = ActivityLog.builder()
                .id(100L)
                .userId(2L)
                .activity(activity)
                .startTime(now)
                .endTime(now.plusMinutes(20))
                .durationMinutes(20L)
                .xpEarned(24.0)
                .notes("ok")
                .createdAt(now)
                .build();

        when(activityLogRepository.findById(100L)).thenReturn(Optional.of(log));

        ResponseEntity<ActivityLogResponse> resp = activityLogService.getActivityLogResponseEntity(100L);

        assertNotNull(resp);
        assertEquals(100L, resp.getBody().id());
        assertEquals(2L, resp.getBody().userId());
        verify(activityLogRepository).findById(100L);
    }

    @Test
    @DisplayName("getActivityLogResponseEntity throws when missing")
    void testGetActivityLogResponseEntityNotFound() {
        when(activityLogRepository.findById(50L)).thenReturn(Optional.empty());

        assertThrows(ActivityNotFoundException.class, () -> activityLogService.getActivityLogResponseEntity(50L));
    }

    @Test
    @DisplayName("addActivityLogResponseResponseEntity saves, notifies gamification and returns response")
    void testAddActivityLogResponseResponseEntity() {
        LocalDateTime now = LocalDateTime.now();
        Long userId = 2L;
        // IDOR fix: userId is passed explicitly (from the trusted header), not on the
        // request body, and is forwarded explicitly to the Feign call too.
        // ActivityLogRequest request = new ActivityLogRequest(
        //         2L,
        //         "Run",
        //         now,
        //         now.plusMinutes(30),
        //         "nice",
        //         now
        // );
        ActivityLogRequest request = new ActivityLogRequest(
                "Run",
                now,
                now.plusMinutes(30),
                "nice",
                now
        );

        Activity activity = Activity.builder().id(7L).name("Run").category(Category.HEALTH).xpMultiplier(2.0).active(true).createdAt(now).build();

        when(activityRepository.findByName("Run")).thenReturn(Optional.of(activity));
        when(activityLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        // when(gamificationClient.createLevelTracker(any())).thenReturn(null);
        when(gamificationClient.createLevelTracker(anyLong(), any())).thenReturn(null);

        // ResponseEntity<ActivityLogResponse> resp = activityLogService.addActivityLogResponseResponseEntity(request);
        ResponseEntity<ActivityLogResponse> resp = activityLogService.addActivityLogResponseResponseEntity(userId, request);

        assertNotNull(resp);
        ActivityLogResponse body = resp.getBody();
        assertEquals(2L, body.userId());
        assertEquals(activity.getId(), body.activity().getId());
        assertNotNull(body.xpEarned());

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<LevelTrackerRequestDTO> captor = ArgumentCaptor.forClass(LevelTrackerRequestDTO.class);
        verify(gamificationClient).createLevelTracker(userIdCaptor.capture(), captor.capture());

        LevelTrackerRequestDTO sent = captor.getValue();
        assertEquals(body.userId(), userIdCaptor.getValue());
        assertEquals(body.activity().getId(), sent.activityId());
        assertEquals(body.xpEarned(), sent.xp(), 1e-6);
    }

    @Test
    @DisplayName("addActivityLogResponseResponseEntity throws when activity not found")
    void testAddActivityLogResponseResponseEntityActivityMissing() {
        LocalDateTime now = LocalDateTime.now();
        // ActivityLogRequest request = new ActivityLogRequest(
        //         2L,
        //         "Missing",
        //         now,
        //         now.plusMinutes(30),
        //         "notes",
        //         now
        // );
        ActivityLogRequest request = new ActivityLogRequest(
                "Missing",
                now,
                now.plusMinutes(30),
                "notes",
                now
        );

        when(activityRepository.findByName("Missing")).thenReturn(Optional.empty());

        assertThrows(ActivityNotFoundException.class, () -> activityLogService.addActivityLogResponseResponseEntity(2L, request));
    }

    @Test
    @DisplayName("getAllActivityForUser returns mapped list")
    void testGetAllActivityForUser() {
        LocalDateTime now = LocalDateTime.now();
        Activity a = Activity.builder().id(1L).name("A").category(Category.OTHER).xpMultiplier(1.0).active(true).createdAt(now).build();

        ActivityLog l1 = ActivityLog.builder().id(1L).userId(2L).activity(a).startTime(now).endTime(now.plusMinutes(10)).durationMinutes(10L).xpEarned(10.0).notes("n").createdAt(now).build();
        ActivityLog l2 = ActivityLog.builder().id(2L).userId(2L).activity(a).startTime(now.plusHours(1)).endTime(now.plusHours(1).plusMinutes(20)).durationMinutes(20L).xpEarned(20.0).notes("n").createdAt(now).build();

        when(activityLogRepository.findByUserId(2L)).thenReturn(List.of(l1, l2));

        ResponseEntity<List<ActivityLogResponse>> resp = activityLogService.getAllActivityForUser(2L);

        assertNotNull(resp);
        assertEquals(2, resp.getBody().size());
        verify(activityLogRepository).findByUserId(2L);
    }
}

