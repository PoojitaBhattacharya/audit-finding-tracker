package com.internship.tool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.tool.entity.AuditFinding;
import com.internship.tool.repository.AuditFindingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuditFindingCrudTestcontainersIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auditdb")
            .withUsername("audit")
            .withPassword("audit");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditFindingRepository repository;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void fullCrudFlowWithRealPostgresAndRedisContainers() throws Exception {
        assertThat(redisConnectionFactory).isNotNull();

        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("title", "Privilege Escalation Risk");
        createRequest.put("description", "Missing authorization check on admin route");
        createRequest.put("severity", "HIGH");
        createRequest.put("status", "CLOSED");
        createRequest.put("dueDate", "2026-05-20");

        String createResponse = mockMvc.perform(post("/findings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Privilege Escalation Risk"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuditFinding created = objectMapper.readValue(createResponse, AuditFinding.class);

        mockMvc.perform(get("/findings")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(created.getId()))
                .andExpect(jsonPath("$.content[0].title").value("Privilege Escalation Risk"));

        mockMvc.perform(get("/findings/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.dueDate").value("2026-05-20"));

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("title", "Privilege Escalation Risk (Updated)");
        updateRequest.put("description", "Authorization guard added and validated");
        updateRequest.put("severity", "MEDIUM");
        updateRequest.put("status", "IN_PROGRESS");
        updateRequest.put("dueDate", "2026-05-25");

        mockMvc.perform(put("/findings/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.title").value("Privilege Escalation Risk (Updated)"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.dueDate").value("2026-05-25"));

        AuditFinding persisted = repository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getTitle()).isEqualTo("Privilege Escalation Risk (Updated)");
        assertThat(persisted.getSeverity()).isEqualTo("MEDIUM");
        assertThat(persisted.getDueDate()).isEqualTo(LocalDate.of(2026, 5, 25));

        mockMvc.perform(delete("/findings/{id}", created.getId()))
                .andExpect(status().isOk());

        assertThat(repository.findById(created.getId())).isEmpty();
    }
}
