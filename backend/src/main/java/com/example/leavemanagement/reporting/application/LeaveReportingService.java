package com.example.leavemanagement.reporting.application;

import com.example.leavemanagement.reporting.persistence.LeaveSummaryRepository;
import com.example.leavemanagement.reporting.persistence.OrganizationLeaveRequestRepository;
import com.example.leavemanagement.shared.api.DomainException;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LeaveReportingService {
    private static final java.util.Set<String> STATUSES = java.util.Set.of("PENDING", "APPROVED", "REJECTED", "CANCELLED");
    private final OrganizationLeaveRequestRepository requests;
    private final LeaveSummaryRepository summaries;
    private final CurrentActorProvider actors;
    public LeaveReportingService(OrganizationLeaveRequestRepository requests, LeaveSummaryRepository summaries, CurrentActorProvider actors) { this.requests = requests; this.summaries = summaries; this.actors = actors; }

    public PageView list(LocalDate from, LocalDate to, String status, int page, int size) {
        admin(); validatePeriod(from, to); validatePage(page, size); if (status != null && !STATUSES.contains(status)) throw new DomainException(400, "VALIDATION_FAILED", "Unknown leave status");
        var result = requests.search(from, to, status, PageRequest.of(page, size));
        return new PageView(result.getContent().stream().map(this::summary).toList(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
    public SummaryReport summary(LocalDate from, LocalDate to) {
        admin(); validatePeriod(from, to);
        return new SummaryReport(from, to, summaries.byStatus(from, to).stream().map(this::bucket).toList(), summaries.byLeaveType(from, to).stream().map(this::bucket).toList());
    }
    private LeaveRequestSummary summary(OrganizationLeaveRequestRepository.Row r) { return new LeaveRequestSummary(r.id(), r.employeeId(), r.employeeName(), r.leaveTypeId(), r.leaveTypeName(), r.startDate(), r.endDate(), r.durationMode(), r.chargeableDays(), r.status(), r.submittedAt(), r.version()); }
    private SummaryBucket bucket(LeaveSummaryRepository.Bucket b) { return new SummaryBucket(b.key(), b.requestCount(), b.chargeableDays()); }
    private void admin() { if (!actors.require().hasRole("ADMINISTRATOR")) throw new DomainException(403, "ACCESS_DENIED", "Administrator role is required"); }
    private void validatePeriod(LocalDate from, LocalDate to) { if (from == null || to == null || to.isBefore(from)) throw new DomainException(400, "VALIDATION_FAILED", "A valid inclusive reporting period is required"); }
    private void validatePage(int page, int size) { if (page < 0 || size < 1 || size > 100) throw new DomainException(400, "VALIDATION_FAILED", "Page must be non-negative and size must be between 1 and 100"); }
    public record LeaveRequestSummary(UUID id, UUID employeeId, String employeeName, UUID leaveTypeId, String leaveTypeName, LocalDate startDate, LocalDate endDate, String durationMode, double chargeableDays, String status, java.time.Instant submittedAt, long version) {}
    public record PageView(List<LeaveRequestSummary> content, int page, int size, long totalElements, int totalPages) {}
    public record SummaryReport(LocalDate from, LocalDate to, List<SummaryBucket> byStatus, List<SummaryBucket> byLeaveType) {}
    public record SummaryBucket(String key, long requestCount, double chargeableDays) {}
}
