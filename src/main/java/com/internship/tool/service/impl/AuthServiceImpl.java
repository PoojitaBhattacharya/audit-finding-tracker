package com.internship.tool.service.impl;

import com.internship.tool.dto.AuthResponse;
import com.internship.tool.dto.LoginRequest;
import com.internship.tool.dto.RegisterRequest;
import com.internship.tool.entity.User;
import com.internship.tool.entity.User.AccountStatus;
import com.internship.tool.entity.User.UserRole;
import com.internship.tool.repository.UserRepository;
import com.internship.tool.security.JwtProvider;
import com.internship.tool.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is inactive");
        }

        String token = jwtProvider.generateToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtProvider.generateToken(user.getUsername(), user.getRole().name());

        log.info("User {} logged in successfully", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .role(user.getRole().name())
                .message("Login successful")
                .build();
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.VIEWER)
                .status(AccountStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtProvider.generateToken(savedUser.getUsername(), savedUser.getRole().name());

        log.info("User {} registered successfully with VIEWER role", savedUser.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(savedUser.getUsername())
                .role(savedUser.getRole().name())
                .message("Registration successful")
                .build();
    }

    @Override
    public AuthResponse refresh(String token) {
        if (!jwtProvider.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        String username = jwtProvider.getUsernameFromToken(token);
        String role = jwtProvider.getRoleFromToken(token);

        String newToken = jwtProvider.generateToken(username, role);

        log.info("Token refreshed for user {}", username);

        return AuthResponse.builder()
                .token(newToken)
                .username(username)
                .role(role)
                .message("Token refreshed")
                .build();
    }
}
