package com.internship.tool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.tool.dto.LoginRequest;
import com.internship.tool.dto.RegisterRequest;
import com.internship.tool.entity.User;
import com.internship.tool.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
    }

    @Test
    public void testRegister_WithValidData_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    public void testRegister_WithDuplicateUsername_BadRequest() throws Exception {
        // Create existing user
        User existing = User.builder()
                .username("existinguser")
                .email("existing@example.com")
                .password(passwordEncoder.encode("pass123"))
                .role(User.UserRole.VIEWER)
                .status(User.AccountStatus.ACTIVE)
                .build();
        userRepository.save(existing);

        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("newemail@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegister_WithInvalidEmail_BadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("invalid-email")
                .password("password123")
                .build();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegister_WithShortPassword_BadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("pass")
                .build();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLogin_WithValidCredentials_Success() throws Exception {
        // Create user with known password
        User user = User.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(User.UserRole.VIEWER)
                .status(User.AccountStatus.ACTIVE)
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    public void testLogin_WithInvalidUsername_Unauthorized() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("nonexistent")
                .password("password123")
                .build();

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testLogin_WithInvalidPassword_Unauthorized() throws Exception {
        // Create user
        User user = User.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password(passwordEncoder.encode("correctpassword"))
                .role(User.UserRole.VIEWER)
                .status(User.AccountStatus.ACTIVE)
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testLogin_WithInactiveAccount_Unauthorized() throws Exception {
        // Create inactive user
        User user = User.builder()
                .username("inactiveuser")
                .email("inactive@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(User.UserRole.VIEWER)
                .status(User.AccountStatus.INACTIVE)
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .username("inactiveuser")
                .password("password123")
                .build();

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testLoginReturnsCorrectRoles() throws Exception {
        // Test ADMIN role
        User admin = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.UserRole.ADMIN)
                .status(User.AccountStatus.ACTIVE)
                .build();
        userRepository.save(admin);

        LoginRequest request = LoginRequest.builder()
                .username("admin")
                .password("admin123")
                .build();

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
