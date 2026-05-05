package com.internship.tool.controller;

import com.internship.tool.dto.DashboardStatsDto;
import com.internship.tool.entity.AuditFinding;
import com.internship.tool.service.AuditFindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/findings")
public class AuditFindingController {

    @Autowired
    private AuditFindingService service;

    // Create - ADMIN and MANAGER can create
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AuditFinding> create(@RequestBody AuditFinding finding) {
        AuditFinding created = service.createFinding(finding);
        return ResponseEntity.ok(created);
    }

    // Get All with pagination + sorting - All authenticated users
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AuditFinding>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Page<AuditFinding> results = service.getAllFindings(page, size, sortBy, sortDir);
        return ResponseEntity.ok(results);
    }

    // Search findings - All authenticated users
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AuditFinding>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AuditFinding> results = service.searchFindings(q, page, size);
        return ResponseEntity.ok(results);
    }

    // Dashboard stats - ADMIN and MANAGER only
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<DashboardStatsDto> getStats() {
        DashboardStatsDto stats = service.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    // Export CSV - ADMIN and MANAGER only
    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        String csvContent = service.exportFindingsAsCsv(sortBy, sortDir);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-findings.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent.getBytes(StandardCharsets.UTF_8));
    }

    // Get by ID - All authenticated users
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuditFinding> getById(@PathVariable Long id) {
        AuditFinding finding = service.getFindingById(id);
        return ResponseEntity.ok(finding);
    }

    // Update - ADMIN and MANAGER only
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AuditFinding> update(@PathVariable Long id, @RequestBody AuditFinding finding) {
        AuditFinding updated = service.updateFinding(id, finding);
        return ResponseEntity.ok(updated);
    }

    // Soft Delete - ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDeleteFinding(id);
        return ResponseEntity.noContent().build();
    }
}
