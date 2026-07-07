package com.tracker.gateway.dto;

import com.tracker.gateway.user.Role;

public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        Role role
) {
}
