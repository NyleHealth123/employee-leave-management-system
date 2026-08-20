package com.example.leavemanagement.request.domain;

import com.example.leavemanagement.policy.persistence.LeavePolicyVersionEntity;
import com.example.leavemanagement.request.persistence.LeaveRequestEntity;
import com.example.leavemanagement.shared.api.DomainException;
import java.time.*;

/** Pure eligibility rules for employee self-cancellation. */
public final class CancellationPolicy {
    private CancellationPolicy() {}
    public static Eligibility evaluate(LeaveRequestEntity request, LeavePolicyVersionEntity policy, UUIDOwner owner, Instant now, ZoneId zone) {
        if (!owner.owns(request)) return new Eligibility(false, "Only the request owner may cancel");
        if ("PENDING".equals(request.getStatus())) return new Eligibility(true, null);
        if (!"APPROVED".equals(request.getStatus())) return new Eligibility(false, "Only pending or approved requests may be cancelled");
        Instant cutoff = request.getStartDate().atStartOfDay(zone).minusDays(policy.getCancellationCutoffDays()).toInstant();
        return now.isBefore(cutoff) ? new Eligibility(true, null) : new Eligibility(false, "The cancellation cutoff has passed");
    }
    public interface UUIDOwner { boolean owns(LeaveRequestEntity request); }
    public record Eligibility(boolean allowed, String blockedReason) {}
}
