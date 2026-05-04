package com.internship.tool.repository;

import com.internship.tool.entity.AuditFinding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditFindingRepository extends JpaRepository<AuditFinding, Long> {
    List<AuditFinding> findByDueDateBeforeAndStatusIgnoreCaseNot(LocalDate dueDate, String status);

    List<AuditFinding> findByDueDateBetweenAndStatusIgnoreCaseNot(LocalDate startDate, LocalDate endDate, String status);

    long countByStatusIgnoreCase(String status);

    long countByDueDateBeforeAndStatusIgnoreCaseNot(LocalDate dueDate, String status);

    List<AuditFinding> findBySeverityIgnoreCase(String severity);

    List<AuditFinding> findByStatusIgnoreCase(String status);

    @Query("SELECT a FROM AuditFinding a WHERE a.deletedAt IS NULL AND LOWER(a.status) = LOWER(:status)")
    Page<AuditFinding> findActiveByStatus(String status, Pageable pageable);

    @Query("SELECT a FROM AuditFinding a WHERE a.deletedAt IS NULL AND a.dueDate BETWEEN :fromDate AND :toDate")
    Page<AuditFinding> findActiveByDueDateRange(LocalDate fromDate, LocalDate toDate, Pageable pageable);

    Page<AuditFinding> findByDeletedAtIsNull(Pageable pageable);

    List<AuditFinding> findAllByDeletedAtIsNull(Sort sort);

    Optional<AuditFinding> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT a FROM AuditFinding a WHERE a.deletedAt IS NULL AND (" +
            "LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.severity) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.status) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<AuditFinding> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrSeverityContainingIgnoreCaseOrStatusContainingIgnoreCase(
            String query, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditFinding a WHERE a.deletedAt IS NULL")
    long countTotalFindings();

    @Query("SELECT COUNT(a) FROM AuditFinding a WHERE a.deletedAt IS NULL AND UPPER(a.status) = 'OPEN'")
    long countOpenFindings();

    @Query("SELECT COUNT(a) FROM AuditFinding a WHERE a.deletedAt IS NULL AND UPPER(a.status) = 'CLOSED'")
    long countClosedFindings();

    @Query("SELECT COUNT(a) FROM AuditFinding a WHERE a.deletedAt IS NULL AND a.dueDate < CURRENT_DATE AND UPPER(a.status) != 'CLOSED'")
    long countOverdueFindings();

    @Query("SELECT COUNT(a) FROM AuditFinding a WHERE a.deletedAt IS NULL AND UPPER(a.severity) = 'CRITICAL'")
    long countCriticalFindings();

    @Query("SELECT COUNT(a) FROM AuditFinding a WHERE a.deletedAt IS NULL AND UPPER(a.severity) = 'HIGH'")
    long countHighFindings();

    @Query("SELECT COUNT(a) FROM AuditFinding a WHERE a.deletedAt IS NULL AND UPPER(a.severity) = 'MEDIUM'")
    long countMediumFindings();

    @Query("SELECT COUNT(a) FROM AuditFinding a WHERE a.deletedAt IS NULL AND UPPER(a.severity) = 'LOW'")
    long countLowFindings();
}
