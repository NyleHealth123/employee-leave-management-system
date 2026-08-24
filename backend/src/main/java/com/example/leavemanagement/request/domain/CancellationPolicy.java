package com.example.leavemanagement.request.domain;

import java.time.*;

/** Pure eligibility rules for employee self-cancellation. */
public final class CancellationPolicy {
    private CancellationPolicy() {}
    public static Eligibility evaluate(boolean owned, String status, LocalDate startDate,
                                       int cancellationCutoffDays, Instant now, ZoneId zone) {
        if (!owned) return new Eligibility(false, "Only the request owner may cancel");
        if ("PENDING".equals(status)) return new Eligibility(true, null);
        if (!"APPROVED".equals(status)) return new Eligibility(false, "Only pending or approved requests may be cancelled");
        Instant cutoff = startDate.atStartOfDay(zone).minusDays(cancellationCutoffDays).toInstant();
        return now.isBefore(cutoff) ? new Eligibility(true, null) : new Eligibility(false, "The cancellation cutoff has passed");
    }
    public record Eligibility(boolean allowed, String blockedReason) {}
}
