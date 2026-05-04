package com.internship.tool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.tool.entity.AuditFinding;
import com.internship.tool.entity.User;
import com.internship.tool.repository.AuditFindingRepository;
import com.internship.tool.repository.UserRepository;
import com.internship.tool.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuditFindingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditFindingRepository auditFindingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String managerToken;
    private String viewerToken;
    private AuditFinding testFinding;

    @BeforeEach
    public void setup() {
        // Clear existing data
        auditFindingRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        User admin = User.builder()
                .username("admin")
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.UserRole.ADMIN)
                .status(User.AccountStatus.ACTIVE)
                .build();
        userRepository.save(admin);
        adminToken = jwtProvider.generateToken("admin", "ADMIN");

        User manager = User.builder()
                .username("manager")
                .email("manager@test.com")
                .password(passwordEncoder.encode("manager123"))
                .role(User.UserRole.MANAGER)
                .status(User.AccountStatus.ACTIVE)
                .build();
        userRepository.save(manager);
        managerToken = jwtProvider.generateToken("manager", "MANAGER");

        User viewer = User.builder()
                .username("viewer")
                .email("viewer@test.com")
                .password(passwordEncoder.encode("viewer123"))
                .role(User.UserRole.VIEWER)
                .status(User.AccountStatus.ACTIVE)
                .build();
        userRepository.save(viewer);
        viewerToken = jwtProvider.generateToken("viewer", "VIEWER");

        // Create test finding
        testFinding = AuditFinding.builder()
                .title("Test Finding")
                .description("Test Description")
                .severity("HIGH")
                .status("OPEN")
                .dueDate(LocalDate.now().plusDays(7))
                .build();
    }

    @Test
    public void testCreateFinding_WithAdminRole_Success() throws Exception {
        mockMvc.perform(post("/findings")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testFinding)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Finding"))
                .andExpect(jsonPath("$.severity").value("HIGH"));
    }

    @Test
    public void testCreateFinding_WithViewerRole_Forbidden() throws Exception {
        mockMvc.perform(post("/findings")
                .header("Authorization", "Bearer " + viewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testFinding)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetAll_Authenticated_Success() throws Exception {
        auditFindingRepository.save(testFinding);

        mockMvc.perform(get("/findings")
                .header("Authorization", "Bearer " + viewerToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Test Finding"));
    }

    @Test
    public void testGetAll_Unauthenticated_Unauthorized() throws Exception {
        mockMvc.perform(get("/findings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetById_Authenticated_Success() throws Exception {
        AuditFinding saved = auditFindingRepository.save(testFinding);

        mockMvc.perform(get("/findings/" + saved.getId())
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.title").value("Test Finding"));
    }

    @Test
    public void testGetById_NotFound() throws Exception {
        mockMvc.perform(get("/findings/999")
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateFinding_WithManagerRole_Success() throws Exception {
        AuditFinding saved = auditFindingRepository.save(testFinding);
        saved.setTitle("Updated Title");
        saved.setStatus("CLOSED");

        mockMvc.perform(put("/findings/" + saved.getId())
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    public void testDeleteFinding_WithAdminRole_Success() throws Exception {
        AuditFinding saved = auditFindingRepository.save(testFinding);

        mockMvc.perform(delete("/findings/" + saved.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify soft delete
        AuditFinding deleted = auditFindingRepository.findById(saved.getId()).orElseThrow();
        assert deleted.getDeletedAt() != null;
    }

    @Test
    public void testDeleteFinding_WithManagerRole_Forbidden() throws Exception {
        AuditFinding saved = auditFindingRepository.save(testFinding);

        mockMvc.perform(delete("/findings/" + saved.getId())
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSearch_WithValidQuery_Success() throws Exception {
        auditFindingRepository.save(testFinding);

        mockMvc.perform(get("/findings/search")
                .header("Authorization", "Bearer " + viewerToken)
                .param("q", "Test")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Test Finding"));
    }

    @Test
    public void testGetStats_WithManagerRole_Success() throws Exception {
        auditFindingRepository.save(testFinding);

        mockMvc.perform(get("/findings/stats")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFindings").value(1))
                .andExpect(jsonPath("$.openFindings").value(1))
                .andExpect(jsonPath("$.closedFindings").value(0));
    }

    @Test
    public void testGetStats_WithViewerRole_Forbidden() throws Exception {
        mockMvc.perform(get("/findings/stats")
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testExportCsv_WithAdminRole_Success() throws Exception {
        auditFindingRepository.save(testFinding);

        mockMvc.perform(get("/findings/export")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentType("text/csv"));
    }
}
