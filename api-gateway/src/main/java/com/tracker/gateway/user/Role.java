package com.tracker.gateway.user;

public enum Role {
    USER,
    ADMIN;

    public String authority() {
        return switch (this) {
            case USER -> "ROLE_USER";
            case ADMIN -> "ROLE_ADMIN";
        };
    }
}
