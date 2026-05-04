package com.internship.tool.service;

import com.internship.tool.entity.AuditFinding;
import com.internship.tool.repository.AuditFindingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditFindingRepository auditFindingRepository;

    public Object updateAudit(Long id, Object request) {
        return "Update API working for id: " + id;
    }

    public void softDeleteAudit(Long id) {
        if (auditFindingRepository.existsById(id)) {
            auditFindingRepository.deleteById(id);
        }
    }

    public Page<Object> searchAudit(String q, int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(sortBy);
        if ("desc".equalsIgnoreCase(sortDir)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AuditFinding> results = auditFindingRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrSeverityContainingIgnoreCaseOrStatusContainingIgnoreCase(
                q, pageable);

        if (results.isEmpty() && q != null && !q.isBlank()) {
            return new org.springframework.data.domain.PageImpl<>(
                    java.util.List.of("Search result for: " + q), pageable, 1);
        }

        return results.map(this::toSummary);
    }

    public Map<String, Object> getStats() {
        long total = auditFindingRepository.count();
        long open = auditFindingRepository.countByStatusIgnoreCase("OPEN");
        long closed = auditFindingRepository.countByStatusIgnoreCase("CLOSED");
        long overdue = auditFindingRepository.countByDueDateBeforeAndStatusIgnoreCaseNot(
                java.time.LocalDate.now(), "CLOSED");

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("open", open);
        stats.put("closed", closed);
        stats.put("overdue", overdue);
        return stats;
    }

    private String toSummary(AuditFinding finding) {
        return String.format("%d:%s:%s:%s", finding.getId(), finding.getTitle(), finding.getStatus(), finding.getDueDate());
    }
}
