package com.tracker.gateway.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(@RequestBody Map<String, String> req) {
        return authService.register(req.get("email"), req.get("password"));
    }

    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> req) {
        return authService.login(req.get("email"), req.get("password"));
    }
}
