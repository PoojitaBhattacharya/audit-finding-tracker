package com.internship.tool.integration;

import com.internship.tool.entity.AuditFinding;
import com.internship.tool.entity.User;
import com.internship.tool.repository.AuditFindingRepository;
import com.internship.tool.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
public class FullCrudFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("db/migration/init-postgres.sql");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuditFindingRepository auditFindingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        auditFindingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testFullAuditFindingCrudFlow() {
        // CREATE
        AuditFinding finding = AuditFinding.builder()
                .title("Security Vulnerability")
                .description("SQL Injection vulnerability in login form")
                .severity("CRITICAL")
                .status("OPEN")
                .dueDate(LocalDate.now().plusDays(3))
                .build();

        AuditFinding created = auditFindingRepository.save(finding);
        assertNotNull(created.getId());
        assertEquals("Security Vulnerability", created.getTitle());
        assertEquals("CRITICAL", created.getSeverity());
        assertEquals("OPEN", created.getStatus());
        assertNull(created.getDeletedAt());

        // READ
        AuditFinding retrieved = auditFindingRepository.findByIdAndDeletedAtIsNull(created.getId())
                .orElseThrow();
        assertEquals(created.getId(), retrieved.getId());
        assertEquals("SQL Injection vulnerability in login form", retrieved.getDescription());

        // UPDATE
        retrieved.setStatus("IN_PROGRESS");
        retrieved.setDescription("Started remediation work");
        AuditFinding updated = auditFindingRepository.save(retrieved);
        assertEquals("IN_PROGRESS", updated.getStatus());
        assertEquals("Started remediation work", updated.getDescription());

        // Soft DELETE
        updated.setDeletedAt(java.time.LocalDateTime.now());
        auditFindingRepository.save(updated);

        // Verify soft delete (should not be found in regular queries)
        assertTrue(auditFindingRepository.findByIdAndDeletedAtIsNull(created.getId()).isEmpty());

        // But should still exist in database
        assertTrue(auditFindingRepository.findById(created.getId()).isPresent());
    }

    @Test
    public void testMultipleFindingsWithPagination() {
        // Create multiple findings
        for (int i = 1; i <= 15; i++) {
            AuditFinding finding = AuditFinding.builder()
                    .title("Finding " + i)
                    .description("Description " + i)
                    .severity(i % 3 == 0 ? "CRITICAL" : i % 2 == 0 ? "HIGH" : "MEDIUM")
                    .status("OPEN")
                    .dueDate(LocalDate.now().plusDays(i))
                    .build();
            auditFindingRepository.save(finding);
        }

        // Test pagination
        var page = auditFindingRepository.findByDeletedAtIsNull(
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertEquals(15, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(10, page.getContent().size());

        // Get second page
        var secondPage = auditFindingRepository.findByDeletedAtIsNull(
                org.springframework.data.domain.PageRequest.of(1, 10)
        );
        assertEquals(5, secondPage.getContent().size());
    }

    @Test
    public void testFindingsBySeverity() {
        // Create findings with different severities
        AuditFinding critical = AuditFinding.builder()
                .title("Critical Issue")
                .severity("CRITICAL")
                .status("OPEN")
                .build();
        AuditFinding high = AuditFinding.builder()
                .title("High Issue")
                .severity("HIGH")
                .status("OPEN")
                .build();
        AuditFinding medium = AuditFinding.builder()
                .title("Medium Issue")
                .severity("MEDIUM")
                .status("OPEN")
                .build();

        auditFindingRepository.save(critical);
        auditFindingRepository.save(high);
        auditFindingRepository.save(medium);

        // Query by severity
        List<AuditFinding> criticals = auditFindingRepository.findBySeverityIgnoreCase("CRITICAL");
        assertEquals(1, criticals.size());
        assertEquals("Critical Issue", criticals.get(0).getTitle());
    }

    @Test
    public void testOverdueFindings() {
        AuditFinding overdue = AuditFinding.builder()
                .title("Overdue Issue")
                .severity("HIGH")
                .status("OPEN")
                .dueDate(LocalDate.now().minusDays(1))
                .build();

        AuditFinding upcoming = AuditFinding.builder()
                .title("Upcoming Issue")
                .severity("MEDIUM")
                .status("OPEN")
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        auditFindingRepository.save(overdue);
        auditFindingRepository.save(upcoming);

        // Find overdue
        List<AuditFinding> overdues = auditFindingRepository
                .findByDueDateBeforeAndStatusIgnoreCaseNot(LocalDate.now(), "CLOSED");
        assertEquals(1, overdues.size());
        assertEquals("Overdue Issue", overdues.get(0).getTitle());
    }

    @Test
    public void testUserAuthenticationFlow() {
        // Create users with different roles
        User admin = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.UserRole.ADMIN)
                .status(User.AccountStatus.ACTIVE)
                .build();

        User manager = User.builder()
                .username("manager")
                .email("manager@example.com")
                .password(passwordEncoder.encode("manager123"))
                .role(User.UserRole.MANAGER)
                .status(User.AccountStatus.ACTIVE)
                .build();

        User viewer = User.builder()
                .username("viewer")
                .email("viewer@example.com")
                .password(passwordEncoder.encode("viewer123"))
                .role(User.UserRole.VIEWER)
                .status(User.AccountStatus.ACTIVE)
                .build();

        userRepository.save(admin);
        userRepository.save(manager);
        userRepository.save(viewer);

        // Verify users are saved with correct roles
        var savedAdmin = userRepository.findByUsername("admin").orElseThrow();
        assertEquals(User.UserRole.ADMIN, savedAdmin.getRole());

        var savedManager = userRepository.findByUsername("manager").orElseThrow();
        assertEquals(User.UserRole.MANAGER, savedManager.getRole());

        var savedViewer = userRepository.findByUsername("viewer").orElseThrow();
        assertEquals(User.UserRole.VIEWER, savedViewer.getRole());

        // Verify passwords are hashed
        assertNotEquals("admin123", savedAdmin.getPassword());
        assertTrue(passwordEncoder.matches("admin123", savedAdmin.getPassword()));
    }

    @Test
    public void testSearchFindings() {
        AuditFinding finding1 = AuditFinding.builder()
                .title("SQL Injection in Login")
                .description("Critical vulnerability in authentication")
                .severity("CRITICAL")
                .status("OPEN")
                .build();

        AuditFinding finding2 = AuditFinding.builder()
                .title("XSS Vulnerability in Forms")
                .description("Medium vulnerability in user input validation")
                .severity("MEDIUM")
                .status("OPEN")
                .build();

        auditFindingRepository.save(finding1);
        auditFindingRepository.save(finding2);

        // Search for "SQL"
        var results = auditFindingRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrSeverityContainingIgnoreCaseOrStatusContainingIgnoreCase(
                        "SQL",
                        org.springframework.data.domain.PageRequest.of(0, 10)
                );

        assertEquals(1, results.getTotalElements());
        assertEquals("SQL Injection in Login", results.getContent().get(0).getTitle());

        // Search for "vulnerability"
        var vulnResults = auditFindingRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrSeverityContainingIgnoreCaseOrStatusContainingIgnoreCase(
                        "vulnerability",
                        org.springframework.data.domain.PageRequest.of(0, 10)
                );

        assertEquals(2, vulnResults.getTotalElements());
    }

    @Test
    public void testDashboardStats() {
        // Create various findings
        for (int i = 0; i < 5; i++) {
            AuditFinding finding = AuditFinding.builder()
                    .title("Finding " + i)
                    .severity(i == 0 ? "CRITICAL" : i == 1 ? "HIGH" : i == 2 ? "MEDIUM" : "LOW")
                    .status(i < 2 ? "OPEN" : "CLOSED")
                    .dueDate(LocalDate.now().plusDays(i))
                    .build();
            auditFindingRepository.save(finding);
        }

        long total = auditFindingRepository.countTotalFindings();
        long open = auditFindingRepository.countOpenFindings();
        long closed = auditFindingRepository.countClosedFindings();
        long critical = auditFindingRepository.countCriticalFindings();

        assertEquals(5, total);
        assertEquals(2, open);
        assertEquals(3, closed);
        assertEquals(1, critical);
    }
}
