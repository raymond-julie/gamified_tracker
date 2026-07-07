package com.tracker.gateway.auth;

import com.tracker.gateway.dto.LoginRequest;
import com.tracker.gateway.dto.RegisterRequest;
import com.tracker.gateway.user.Role;
import com.tracker.gateway.user.User;
import com.tracker.gateway.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(RegisterRequest request) {

        var user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());

        // Encrypt password before saving
        user.setPassword(passwordEncoder.encode(request.password()));

        user.setRole(Role.USER);
        userRepository.save(user);

        return jwtUtil.generateToken(request.email());
    }

    public String login(LoginRequest req) {

        var user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Compare encrypted password
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtUtil.generateToken(req.email());
    }
}