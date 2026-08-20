package com.example.leavemanagement.reporting.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Read-only, reporting-safe projection of organization requests. */
@Repository
public class OrganizationLeaveRequestRepository {
    @PersistenceContext private EntityManager entityManager;

    public Page<Row> search(LocalDate from, LocalDate to, String status, Pageable pageable) {
        var where = new StringBuilder(" from leave_request r join employee_profile e on e.id=r.employee_id join leave_type lt on lt.id=r.leave_type_id where r.start_date <= :to and r.end_date >= :from");
        if (status != null && !status.isBlank()) where.append(" and r.status = :status");
        var order = " order by r.start_date asc, r.end_date asc, e.display_name asc, r.id asc";
        var query = entityManager.createNativeQuery("select r.id,r.employee_id,e.display_name,r.leave_type_id,lt.name,r.start_date,r.end_date,r.duration_mode,r.chargeable_units,r.status,r.submitted_at,r.version" + where + order);
        var count = entityManager.createNativeQuery("select count(*)" + where);
        query.setParameter("from", from); query.setParameter("to", to);
        count.setParameter("from", from); count.setParameter("to", to);
        if (status != null && !status.isBlank()) { query.setParameter("status", status); count.setParameter("status", status); }
        query.setFirstResult((int) pageable.getOffset()); query.setMaxResults(pageable.getPageSize());
        @SuppressWarnings("unchecked") var rows = (List<Object[]>) query.getResultList();
        var content = rows.stream().map(this::row).toList();
        return new PageImpl<>(content, pageable, ((Number) count.getSingleResult()).longValue());
    }

    private Row row(Object[] x) {
        return new Row((UUID) x[0], (UUID) x[1], (String) x[2], (UUID) x[3], (String) x[4], date(x[5]), date(x[6]), (String) x[7], ((Number) x[8]).intValue() / 2.0, (String) x[9], instant(x[10]), ((Number) x[11]).longValue());
    }
    private LocalDate date(Object value) { return value instanceof LocalDate d ? d : ((Date) value).toLocalDate(); }
    private Instant instant(Object value) { return value instanceof Instant i ? i : ((Timestamp) value).toInstant(); }
    public record Row(UUID id, UUID employeeId, String employeeName, UUID leaveTypeId, String leaveTypeName, LocalDate startDate, LocalDate endDate, String durationMode, double chargeableDays, String status, Instant submittedAt, long version) {}
}
