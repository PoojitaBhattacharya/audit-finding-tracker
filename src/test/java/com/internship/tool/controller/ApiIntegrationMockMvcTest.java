package com.internship.tool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.tool.entity.AuditFinding;
import com.internship.tool.entity.User;
import com.internship.tool.repository.AuditFindingRepository;
import com.internship.tool.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = {"ADMIN"})
class ApiIntegrationMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditFindingRepository auditFindingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        auditFindingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createFindingReturns200AndBodyStructure() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("title", "SQL Injection");
        request.put("description", "Input validation missing");
        request.put("severity", "HIGH");
        request.put("status", "OPEN");
        request.put("dueDate", "2026-05-01");

        mockMvc.perform(post("/findings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("SQL Injection"));
    }

    @Test
    void getAllFindingsReturns200AndPagedBodyStructure() throws Exception {
        auditFindingRepository.save(buildFinding("Issue A", "HIGH", LocalDate.now().plusDays(5)));
        auditFindingRepository.save(buildFinding("Issue B", "MEDIUM", LocalDate.now().plusDays(7)));

        mockMvc.perform(get("/findings")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void exportFindingsCsvReturns200AndCsvStructure() throws Exception {
        auditFindingRepository.save(buildFinding("CSV Issue", "HIGH", LocalDate.now().plusDays(2)));

        mockMvc.perform(get("/findings/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("audit-findings.csv")))
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string(containsString("id,title")));
    }

    @Test
    void getFindingByIdReturns200AndBodyStructure() throws Exception {
        AuditFinding saved = auditFindingRepository.save(buildFinding("ID Lookup", "HIGH", LocalDate.now().plusDays(4)));

        mockMvc.perform(get("/findings/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()));
    }

    @Test
    void updateFindingReturns200AndUpdatedBodyStructure() throws Exception {
        AuditFinding saved = auditFindingRepository.save(buildFinding("Before Update", "HIGH", LocalDate.now().plusDays(3)));

        Map<String, Object> request = new HashMap<>();
        request.put("title", "After Update");
        request.put("severity", "MEDIUM");

        mockMvc.perform(put("/findings/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("After Update"));
    }

    @Test
    void deleteFindingReturns204() throws Exception {
        AuditFinding saved = auditFindingRepository.save(buildFinding("Delete Me", "HIGH", LocalDate.now().plusDays(1)));

        mockMvc.perform(delete("/findings/{id}", saved.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void searchFindingsReturns200AndPagedBodyStructure() throws Exception {
        auditFindingRepository.save(buildFinding("SQL Injection", "HIGH", LocalDate.now().plusDays(5)));
        auditFindingRepository.save(buildFinding("XSS Vulnerability", "MEDIUM", LocalDate.now().plusDays(7)));

        mockMvc.perform(get("/findings/search")
                        .param("q", "SQL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getStatsReturns200AndDashboardStatsStructure() throws Exception {
        auditFindingRepository.save(buildFinding("Critical Issue", "CRITICAL", LocalDate.now().plusDays(1)));
        auditFindingRepository.save(buildFinding("High Issue", "HIGH", LocalDate.now().plusDays(2)));

        mockMvc.perform(get("/findings/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFindings").isNumber())
                .andExpect(jsonPath("$.openFindings").isNumber());
    }

    @Test
    void loginReturns200AndAuthResponseStructure() throws Exception {
        userRepository.save(User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.UserRole.ADMIN)
                .status(User.AccountStatus.ACTIVE)
                .build());

        Map<String, Object> request = new HashMap<>();
        request.put("username", "admin");
        request.put("password", "admin123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void registerReturns200AndAuthResponseStructure() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("username", "newuser");
        request.put("email", "newuser@example.com");
        request.put("password", "password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    private AuditFinding buildFinding(String title, String severity, LocalDate dueDate) {
        return AuditFinding.builder()
                .title(title)
                .description("Test description")
                .severity(severity)
                .status("OPEN")
                .dueDate(dueDate)
                .build();
    }
}
