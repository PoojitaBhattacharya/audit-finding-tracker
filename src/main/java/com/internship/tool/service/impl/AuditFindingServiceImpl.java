package com.internship.tool.service.impl;

import com.internship.tool.dto.DashboardStatsDto;
import com.internship.tool.entity.AuditFinding;
import com.internship.tool.repository.AuditFindingRepository;
import com.internship.tool.service.AuditFindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditFindingServiceImpl implements AuditFindingService {

    @Autowired
    private AuditFindingRepository repository;

    @Override
    public AuditFinding createFinding(AuditFinding finding) {
        if (finding.getStatus() == null) {
            finding.setStatus("OPEN");
        }
        return repository.save(finding);
    }

    @Override
    public Page<AuditFinding> getAllFindings(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(sortBy);
        if ("desc".equalsIgnoreCase(sortDir)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        return repository.findByDeletedAtIsNull(pageable);
    }

    @Override
    public List<AuditFinding> getAllFindings() {
        return repository.findAll();
    }

    @Override
    public AuditFinding getFindingById(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Finding not found"));
    }

    @Override
    public AuditFinding updateFinding(Long id, AuditFinding updatedFinding) {
        AuditFinding existing = getFindingById(id);

        if (updatedFinding.getTitle() != null) {
            existing.setTitle(updatedFinding.getTitle());
        }
        if (updatedFinding.getDescription() != null) {
            existing.setDescription(updatedFinding.getDescription());
        }
        if (updatedFinding.getSeverity() != null) {
            existing.setSeverity(updatedFinding.getSeverity());
        }
        if (updatedFinding.getStatus() != null) {
            existing.setStatus(updatedFinding.getStatus());
        }
        if (updatedFinding.getDueDate() != null) {
            existing.setDueDate(updatedFinding.getDueDate());
        }

        return repository.save(existing);
    }

    @Override
    public void deleteFinding(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void softDeleteFinding(Long id) {
        AuditFinding finding = getFindingById(id);
        finding.setDeletedAt(LocalDateTime.now());
        repository.save(finding);
    }

    @Override
    public String exportFindingsAsCsv(String sortBy, String sortDir) {
        Sort sort = Sort.by(sortBy);
        if ("desc".equalsIgnoreCase(sortDir)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }

        List<AuditFinding> findings = repository.findAllByDeletedAtIsNull(sort);
        StringBuilder csv = new StringBuilder();
        csv.append("id,title,description,severity,status,dueDate\n");
        for (AuditFinding finding : findings) {
            csv.append(finding.getId()).append(',')
                    .append(escapeCsv(finding.getTitle())).append(',')
                    .append(escapeCsv(finding.getDescription())).append(',')
                    .append(escapeCsv(finding.getSeverity())).append(',')
                    .append(escapeCsv(finding.getStatus())).append(',')
                    .append(finding.getDueDate() != null ? finding.getDueDate() : "")
                    .append('\n');
        }
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
            return '"' + escaped + '"';
        }
        return escaped;
    }

    @Override
    public List<AuditFinding> getBySeverity(String severity) {
        return repository.findBySeverityIgnoreCase(severity);
    }

    @Override
    public List<AuditFinding> getByStatus(String status) {
        return repository.findByStatusIgnoreCase(status);
    }

    @Override
    public Page<AuditFinding> searchFindings(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrSeverityContainingIgnoreCaseOrStatusContainingIgnoreCase(
                query, pageable);
    }

    @Override
    public DashboardStatsDto getDashboardStats() {
        return DashboardStatsDto.builder()
                .totalFindings(repository.countTotalFindings())
                .openFindings(repository.countOpenFindings())
                .closedFindings(repository.countClosedFindings())
                .overdueFindings(repository.countOverdueFindings())
                .criticalFindings(repository.countCriticalFindings())
                .highFindings(repository.countHighFindings())
                .mediumFindings(repository.countMediumFindings())
                .lowFindings(repository.countLowFindings())
                .build();
    }
}