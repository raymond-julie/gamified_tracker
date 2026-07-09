package com.tracker.activity.service;

import com.tracker.activity.dto.ActivityResponseRecord;
import com.tracker.activity.dto.ActivityRequestRecord;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ActivityService {
    ResponseEntity<ActivityResponseRecord> getActivity(String name);

    ResponseEntity<List<ActivityResponseRecord>> getAllActivities();

    ResponseEntity<ActivityResponseRecord> addActivityEntity(ActivityRequestRecord request);
}
