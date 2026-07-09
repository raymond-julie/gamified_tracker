package com.tracker.activity.repository;

import com.tracker.activity.dao.Activity;
import com.tracker.activity.dao.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class ActivityRepositoryTest {

    @Autowired
    private ActivityRepository activityRepository;

    @Test
    void testFindByName() {
        Activity a = Activity.builder()
                .name("X")
                .category(Category.WORK)
                .xpMultiplier(1.0)
                .active(true)
                .description("d")
                .createdAt(LocalDateTime.now())
                .build();

        Activity saved = activityRepository.save(a);

        var opt = activityRepository.findByName("X");
        assertTrue(opt.isPresent());
        assertEquals(saved.getId(), opt.get().getId());
    }
}

