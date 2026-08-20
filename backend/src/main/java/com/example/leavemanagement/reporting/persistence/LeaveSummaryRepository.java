package com.example.leavemanagement.reporting.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class LeaveSummaryRepository {
    @PersistenceContext private EntityManager entityManager;

    public List<Bucket> byStatus(LocalDate from, LocalDate to) { return buckets("r.status", from, to); }
    public List<Bucket> byLeaveType(LocalDate from, LocalDate to) { return buckets("lt.name", from, to); }

    private List<Bucket> buckets(String key, LocalDate from, LocalDate to) {
        var sql = "select " + key + ", count(r.id), coalesce(sum(r.chargeable_units),0) from leave_request r join leave_type lt on lt.id=r.leave_type_id where r.start_date <= :to and r.end_date >= :from group by " + key + " order by " + key;
        var query = entityManager.createNativeQuery(sql).setParameter("from", from).setParameter("to", to);
        @SuppressWarnings("unchecked") var rows = (List<Object[]>) query.getResultList();
        return rows.stream().map(x -> new Bucket((String) x[0], ((Number) x[1]).longValue(), ((Number) x[2]).doubleValue() / 2.0)).toList();
    }
    public record Bucket(String key, long requestCount, double chargeableDays) {}
}
