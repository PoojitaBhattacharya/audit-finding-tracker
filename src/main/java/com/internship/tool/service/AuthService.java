package com.internship.tool.service;

import com.internship.tool.dto.AuthResponse;
import com.internship.tool.dto.LoginRequest;
import com.internship.tool.dto.RegisterRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse refresh(String token);
}
