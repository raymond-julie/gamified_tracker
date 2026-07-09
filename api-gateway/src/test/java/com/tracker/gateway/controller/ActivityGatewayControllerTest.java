package com.tracker.gateway.controller;

import com.tracker.gateway.auth.AuthService;
import com.tracker.gateway.auth.JwtUtil;
import com.tracker.gateway.client.ActivityClient;
import com.tracker.gateway.dao.Category;
import com.tracker.gateway.dto.ActivityResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityGatewayController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ActivityGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityClient activityClient;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void testGetAllActivities() throws Exception {
        var response = new ActivityResponse(
                "Study",
                Category.STUDY,
                1.5,
                true,
                "desc",
                LocalDateTime.now()
        );

        when(activityClient.getAllActivities()).thenReturn(ResponseEntity.ok(List.of(response)));

        mockMvc.perform(get("/api/activity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testGetActivity() throws Exception {
        var response = new ActivityResponse(
                "Work",
                Category.WORK,
                1.2,
                true,
                "work desc",
                LocalDateTime.now()
        );

        when(activityClient.getActivity("Work")).thenReturn(ResponseEntity.ok(response));

        mockMvc.perform(get("/api/activity/Work").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAddActivity() throws Exception {
        var response = new ActivityResponse(
                "Gaming",
                Category.GAMING,
                0.8,
                true,
                "fun",
                LocalDateTime.now()
        );

        when(activityClient.addActivity(any())).thenReturn(ResponseEntity.ok(response));

        String json = "{\"name\":\"Gaming\",\"category\":\"GAMING\",\"xpMultiplier\":0.8,\"active\":true,\"description\":\"fun\"}";

        mockMvc.perform(post("/api/activity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}


