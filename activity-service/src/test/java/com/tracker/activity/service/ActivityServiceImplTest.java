package com.tracker.activity.service;

import com.tracker.activity.dao.Activity;
import com.tracker.activity.dao.Category;
import com.tracker.activity.dto.ActivityRequestRecord;
import com.tracker.activity.dto.ActivityResponseRecord;
import com.tracker.activity.exception.ActivityNotFoundException;
import com.tracker.activity.repository.ActivityRepository;
import com.tracker.activity.service.impl.ActivityServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@DisplayName("Activity Service Tests")
public class ActivityServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private ActivityServiceImpl activityService;

    @Test
    @DisplayName("getActivity returns mapped ActivityResponseRecord")
    void testGetActivity() {
        String name = "Running";
        LocalDateTime now = LocalDateTime.now();
        Activity activity = Activity.builder()
                .id(1L)
                .name(name)
                .category(Category.HEALTH)
                .xpMultiplier(1.5)
                .active(true)
                .description("Running activity")
                .createdAt(now)
                .build();

        when(activityRepository.findByName(name)).thenReturn(Optional.of(activity));

        ResponseEntity<ActivityResponseRecord> resp = activityService.getActivity(name);

        assertNotNull(resp);
        assertEquals(name, resp.getBody().name());
        assertEquals(Category.HEALTH, resp.getBody().category());
        verify(activityRepository).findByName(name);
    }

    @Test
    @DisplayName("getActivity throws ActivityNotFoundException when missing")
    void testGetActivityNotFound() {
        String name = "Unknown";
        when(activityRepository.findByName(name)).thenReturn(Optional.empty());

        assertThrows(ActivityNotFoundException.class, () -> activityService.getActivity(name));
        verify(activityRepository).findByName(name);
    }

    @Test
    @DisplayName("addActivityEntity saves and returns response")
    void testAddActivityEntity() {
        LocalDateTime now = LocalDateTime.now();
        ActivityRequestRecord request = new ActivityRequestRecord(
                "Reading",
                Category.STUDY,
                1.5,
                true,
                "Read books",
                now
        );

        when(activityRepository.save(any())).thenAnswer(invocation -> {
            Activity arg = invocation.getArgument(0);
            arg.setId(10L);
            return arg;
        });

        ResponseEntity<ActivityResponseRecord> resp = activityService.addActivityEntity(request);

        assertNotNull(resp);
        assertEquals(request.name(), resp.getBody().name());
        assertEquals(request.category(), resp.getBody().category());
        verify(activityRepository).save(any());
    }

    @Test
    @DisplayName("getAllActivities returns mapped list")
    void testGetAllActivities() {
        LocalDateTime now = LocalDateTime.now();
        Activity a1 = Activity.builder().id(1L).name("A").category(Category.OTHER).xpMultiplier(1.0).active(true).createdAt(now).build();
        Activity a2 = Activity.builder().id(2L).name("B").category(Category.WORK).xpMultiplier(1.5).active(true).createdAt(now).build();

        when(activityRepository.findAll()).thenReturn(List.of(a1, a2));

        ResponseEntity<List<ActivityResponseRecord>> resp = activityService.getAllActivities();

        assertNotNull(resp);
        assertEquals(2, resp.getBody().size());
        verify(activityRepository).findAll();
    }
}

