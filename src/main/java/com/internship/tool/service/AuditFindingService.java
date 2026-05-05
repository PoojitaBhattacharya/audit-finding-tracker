package com.internship.tool.service;

import com.internship.tool.dto.DashboardStatsDto;
import com.internship.tool.entity.AuditFinding;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AuditFindingService {

    AuditFinding createFinding(AuditFinding finding);

    Page<AuditFinding> getAllFindings(int page, int size, String sortBy, String sortDir);

    List<AuditFinding> getAllFindings();

    AuditFinding getFindingById(Long id);

    AuditFinding updateFinding(Long id, AuditFinding finding);

    void deleteFinding(Long id);

    void softDeleteFinding(Long id);

    String exportFindingsAsCsv(String sortBy, String sortDir);

    List<AuditFinding> getBySeverity(String severity);

    List<AuditFinding> getByStatus(String status);

    Page<AuditFinding> searchFindings(String query, int page, int size);

    DashboardStatsDto getDashboardStats();
}